package com.firefly.client.http2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritePendingException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.stream.AbstractHTTP1Connection;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.codec.Base64Utils;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP1ClientConnection extends AbstractHTTP1Connection {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final ResponseHandlerWrap wrap;
	private volatile HTTPClientRequest request;
	private volatile HttpGenerator.Result generatorResult;

	private static class ResponseHandlerWrap extends HTTP1ClientResponseHandler {

		private final AtomicReference<HTTP1ClientResponseHandler> writing = new AtomicReference<>();

		@Override
		public void earlyEOF() {
			writing.get().earlyEOF();
		}

		@Override
		public boolean content(ByteBuffer item, HTTPClientResponse response, HTTP1ClientConnection connection) {
			return writing.get().content(item, response, connection);
		}

		@Override
		public boolean headerComplete(HTTPClientResponse response, HTTP1ClientConnection connection) {
			return writing.get().headerComplete(response, connection);
		}

		@Override
		public boolean messageComplete(HTTPClientResponse response, HTTP1ClientConnection connection) {
			return writing.getAndSet(null).messageComplete(response, connection);
		}

		@Override
		public void badMessage(int status, String reason, HTTPClientResponse response, HTTP1ClientConnection connection) {
			writing.get().badMessage(status, reason, response, connection);
		}

	}

	public HTTP1ClientConnection(HTTP2Configuration config, Session tcpSession, SSLSession sslSession) {
		this(config, sslSession, tcpSession, new ResponseHandlerWrap());
	}

	private HTTP1ClientConnection(HTTP2Configuration config, SSLSession sslSession, Session tcpSession,
			ResponseHandler responseHandler) {
		super(config, sslSession, tcpSession, null, responseHandler);
		wrap = (ResponseHandlerWrap) responseHandler;
	}

	@Override
	protected HttpParser initHttpParser(HTTP2Configuration config, RequestHandler requestHandler,
			ResponseHandler responseHandler) {
		return new HttpParser(responseHandler, config.getMaxRequestHeadLength());
	}

	HttpParser getParser() {
		return parser;
	}

	HttpGenerator getGenerator() {
		return generator;
	}

	SSLSession getSSLSession() {
		return sslSession;
	}
	
	public HTTPClientRequest getRequest() {
		return request;
	}

	void reset() {
		request = null;
		generatorResult = null;
		generator.reset();
		parser.reset();
	}

	public void upgradeHTTP2WithCleartext(HTTPClientRequest request, SettingsFrame settings,
			final Promise<HTTPConnection> promise, final Listener listener, final HTTP1ClientResponseHandler handler) {
		if (isEncrypted()) {
			throw new IllegalStateException("The TLS TCP connection must use ALPN to upgrade HTTP2");
		}

		HTTP1ClientResponseHandler httpResponseHandlerWrap = new HTTP1ClientResponseHandler() {

			@Override
			public void earlyEOF() {
				handler.earlyEOF();
			}

			@Override
			public boolean content(ByteBuffer item, HTTPClientResponse response, HTTP1ClientConnection connection) {
				return handler.content(item, response, connection);
			}

			@Override
			public boolean headerComplete(HTTPClientResponse response, HTTP1ClientConnection connection) {
				return handler.headerComplete(response, connection);
			}

			@Override
			public boolean messageComplete(HTTPClientResponse response, HTTP1ClientConnection connection) {
				String connectionValue = response.getFields().get(HttpHeader.CONNECTION);
				String upgradeValue = response.getFields().get(HttpHeader.UPGRADE);
				if (response.getStatus() == HttpStatus.SWITCHING_PROTOCOLS_101
						&& "Upgrade".equalsIgnoreCase(connectionValue) && "h2c".equalsIgnoreCase(upgradeValue)) {
					
					// initialize http2 client connection;
					final HTTP2ClientConnection http2Connection = new HTTP2ClientConnection(config, tcpSession,
							sslSession, listener);
					tcpSession.attachObject(http2Connection);
					http2Connection.initialize(config, promise, listener);

					return handler.messageComplete(response, connection);
				} else {
					return handler.messageComplete(response, connection);
				}
			}

			@Override
			public void badMessage(int status, String reason, HTTPClientResponse response, HTTP1ClientConnection connection) {
				handler.badMessage(status, reason);
			}
		};
		
		// generate http2 upgrading headers
		request.getFields().add(new HttpField(HttpHeader.CONNECTION, "Upgrade, HTTP2-Settings"));
		request.getFields().add(new HttpField(HttpHeader.UPGRADE, "h2c"));
		if (settings != null) {
			List<ByteBuffer> byteBuffers = http2Generator.control(settings);
			if (byteBuffers != null && byteBuffers.size() > 0) {
				try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
					for (ByteBuffer buffer : byteBuffers) {
						out.write(BufferUtils.toArray(buffer));
					}
					request.getFields().add(new HttpField(HttpHeader.HTTP2_SETTINGS,
							Base64Utils.encodeToUrlSafeString(out.toByteArray())));
				} catch (IOException e) {
					log.error("generate http2 upgrading settings exception", e);
				}
			} else {
				request.getFields().add(new HttpField(HttpHeader.HTTP2_SETTINGS, " "));
			}
		} else {
			request.getFields().add(new HttpField(HttpHeader.HTTP2_SETTINGS, " "));
		}

		request(request, httpResponseHandlerWrap);
	}

	public void request(HTTPClientRequest request, HTTP1ClientResponseHandler handler) {
		requestWithData(request, null, handler);
	}

	public void requestWithData(HTTPClientRequest request, ByteBuffer data, HTTP1ClientResponseHandler handler) {
		checkWrite(request, handler);
		ByteBuffer header = ByteBuffer.allocate(config.getMaxRequestHeadLength());
		try {
			generatorResult = generator.generateRequest(request, header, null, data, true);
			if(generatorResult == HttpGenerator.Result.FLUSH && generator.getState() == HttpGenerator.State.COMPLETING) {
				tcpSession.encode(header);
				tcpSession.encode(data);
				generatorResult = generator.generateRequest(null, null, null, null, true);
				if(generatorResult == HttpGenerator.Result.DONE && generator.getState() == HttpGenerator.State.END) {
					log.debug("client session {} generates the HTTP message completely", tcpSession.getSessionId());
				} else {
					generator.reset();
					throw new IllegalStateException("Client generates http message exception.");
				}
			} else {
				generator.reset();
				throw new IllegalStateException("Client generates http message exception.");
			}
		} catch (IOException e) {
			generator.reset();
			log.error("client generates the HTTP message exception", e);
		}
	}

	public void requestWithByteBufferArray(HTTPClientRequest request, ByteBuffer[] dataArray, HTTP1ClientResponseHandler handler) {
		if(dataArray == null || dataArray.length == 0) {
			request(request, handler);
		} else if(dataArray.length == 1) {
			requestWithData(request, dataArray[0], handler);
		} else {
			checkWrite(request, handler);
			ByteBuffer header = ByteBuffer.allocate(config.getMaxRequestHeadLength());
			try {
				generatorResult = generator.generateRequest(request, header, null, dataArray[0], false);
				if(generatorResult == HttpGenerator.Result.FLUSH && generator.getState() == HttpGenerator.State.COMMITTED) {
					tcpSession.encode(header);
					tcpSession.encode(dataArray[0]);
				} else {
					generator.reset();
					throw new IllegalStateException("Client generates http message exception.");
				}
				
				for (int i = 1; i < dataArray.length; i++) {
					ByteBuffer data = dataArray[i];
					if(generator.isChunking()) {
						// TODO
					} else {
						generatorResult = generator.generateRequest(request, header, null, data, false);
						if(generatorResult == HttpGenerator.Result.FLUSH && generator.getState() == HttpGenerator.State.COMMITTED) {
							tcpSession.encode(header);
							tcpSession.encode(data);
						} else {
							generator.reset();
							throw new IllegalStateException("Client generates http message exception.");
						}
					}
				}
				
				if(generator.isChunking()) {
					// TODO
				} else {
					generatorResult = generator.generateRequest(null, null, null, null, true);
					if(generatorResult == HttpGenerator.Result.CONTINUE && generator.getState() == HttpGenerator.State.COMPLETING) {
						generatorResult = generator.generateRequest(null, null, null, null, true);
						if(generatorResult == HttpGenerator.Result.DONE && generator.getState() == HttpGenerator.State.END) {
							
						}
					}
				}
			} catch (IOException e) {
				generator.reset();
				log.error("client generates the HTTP header exception", e);
			}
		}
		
	}

	public OutputStream requestWithStream(HTTPClientRequest request, HTTP1ClientResponseHandler handler) {
		checkWrite(request, handler);
		// TODO
		
		return null;
	}

	private void checkWrite(HTTPClientRequest request, HTTP1ClientResponseHandler handler) {
		if (wrap.writing.compareAndSet(null, handler)) {
			handler.connection = this;
			this.request = request;
		} else {
			throw new WritePendingException();
		}
	}

}
