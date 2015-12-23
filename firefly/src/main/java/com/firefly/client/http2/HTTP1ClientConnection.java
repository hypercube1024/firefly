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

	private static class ResponseHandlerWrap extends HTTPResponseHandler {

		private final AtomicReference<HTTPResponseHandler> writing = new AtomicReference<>();

		@Override
		public void earlyEOF() {
			writing.get().earlyEOF();
		}

		@Override
		public boolean content(ByteBuffer item, HTTPResponse response, HTTPConnection connection) {
			return writing.get().content(item, response, connection);
		}

		@Override
		public boolean headerComplete(HTTPResponse response, HTTPConnection connection) {
			return writing.get().headerComplete(response, connection);
		}

		@Override
		public boolean messageComplete(HTTPResponse response, HTTPConnection connection) {
			return writing.getAndSet(null).messageComplete(response, connection);
		}

		@Override
		public void badMessage(int status, String reason, HTTPResponse response, HTTPConnection connection) {
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

	public void upgradeHTTP2WithCleartext(HTTPRequest request, SettingsFrame settings,
			final Promise<HTTPConnection> promise, final Listener listener, final HTTPResponseHandler handler) {
		if (isEncrypted()) {
			throw new IllegalStateException("The TLS TCP connection must use ALPN to upgrade HTTP2");
		}

		HTTPResponseHandler httpResponseHandlerWrap = new HTTPResponseHandler() {

			@Override
			public void earlyEOF() {
				handler.earlyEOF();
			}

			@Override
			public boolean content(ByteBuffer item, HTTPResponse response, HTTPConnection connection) {
				return handler.content(item, response, connection);
			}

			@Override
			public boolean headerComplete(HTTPResponse response, HTTPConnection connection) {
				return handler.headerComplete(response, connection);
			}

			@Override
			public boolean messageComplete(HTTPResponse response, HTTPConnection connection) {
				String connectionValue = response.getHttpFields().get(HttpHeader.CONNECTION);
				String upgradeValue = response.getHttpFields().get(HttpHeader.UPGRADE);
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
			public void badMessage(int status, String reason, HTTPResponse response, HTTPConnection connection) {
				handler.badMessage(status, reason);
			}
		};

		checkWrite(httpResponseHandlerWrap);
		
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

		tcpSession.encode(request);
	}

	public void request(HTTPRequest request, HTTPResponseHandler handler) {
		checkWrite(handler);
		tcpSession.encode(request);
	}

	public void request(HTTPRequest request, ByteBuffer data, HTTPResponseHandler handler) {
		checkWrite(handler);
		tcpSession.encode(request);
		tcpSession.encode(data);
	}

	public void request(HTTPRequest request, ByteBuffer[] data, HTTPResponseHandler handler) {
		checkWrite(handler);
		tcpSession.encode(request);
		tcpSession.encode(data);
	}

	public OutputStream requestWithStream(HTTPRequest request, HTTPResponseHandler handler) {
		checkWrite(handler);
		return new OutputStream() {

			@Override
			public void write(byte b[], int off, int len) throws IOException {
				ByteBuffer buffer = BufferUtils.allocate(len);
				BufferUtils.append(buffer, b, off, len);
				tcpSession.encode(buffer);
			}

			@Override
			public void write(int b) throws IOException {
				ByteBuffer buffer = BufferUtils.allocate(1);
				BufferUtils.append(buffer, (byte) b);
				tcpSession.encode(buffer);
			}
		};
	}

	private void checkWrite(HTTPResponseHandler handler) {
		if (wrap.writing.compareAndSet(null, handler)) {
			handler.connection = this;
		} else {
			throw new WritePendingException();
		}
	}

}
