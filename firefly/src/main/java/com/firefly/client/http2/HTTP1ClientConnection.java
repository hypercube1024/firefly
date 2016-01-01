package com.firefly.client.http2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritePendingException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.firefly.codec.http2.decode.HttpParser;
import com.firefly.codec.http2.decode.HttpParser.RequestHandler;
import com.firefly.codec.http2.decode.HttpParser.ResponseHandler;
import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpHeaderValue;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.stream.AbstractHTTP1Connection;
import com.firefly.codec.http2.stream.AbstractHTTP1OutputStream;
import com.firefly.codec.http2.stream.FlowControlStrategy;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTP2Session;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.codec.Base64Utils;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP1ClientConnection extends AbstractHTTP1Connection implements HTTPClientConnection {

	private static final Log log = LogFactory.getInstance().getLog("firefly-system");

	private Promise<HTTPClientConnection> http2ConnectionPromise;
	private Listener http2Sessionlistener;
	private Promise<Stream> initStream;
	private Stream.Listener initStreamListener;
	private volatile boolean upgradeHTTP2Successfully = false;

	private final ResponseHandlerWrap wrap;

	private static class ResponseHandlerWrap implements ResponseHandler {

		private final AtomicReference<HTTP1ClientResponseHandler> writing = new AtomicReference<>();
		private int status;
		private String reason;
		private HTTP1ClientConnection connection;

		@Override
		public void earlyEOF() {
			writing.get().earlyEOF();
		}

		@Override
		public boolean content(ByteBuffer item) {
			return writing.get().content(item);
		}

		@Override
		public boolean headerComplete() {
			return writing.get().headerComplete();
		}

		@Override
		public boolean messageComplete() {
			if (status == 100 && "Continue".equalsIgnoreCase(reason)) {
				log.debug("client received the 100 Continue response");
				connection.getParser().reset();
				return true;
			} else {
				return writing.getAndSet(null).messageComplete();
			}
		}

		@Override
		public void parsedHeader(HttpField field) {
			writing.get().parsedHeader(field);
		}

		@Override
		public void badMessage(int status, String reason) {
			writing.get().badMessage(status, reason);
		}

		@Override
		public int getHeaderCacheSize() {
			return 1024;
		}

		@Override
		public boolean startResponse(HttpVersion version, int status, String reason) {
			this.status = status;
			this.reason = reason;
			return writing.get().startResponse(version, status, reason);
		}

	}

	public HTTP1ClientConnection(HTTP2Configuration config, Session tcpSession, SSLSession sslSession) {
		this(config, sslSession, tcpSession, new ResponseHandlerWrap());
	}

	private HTTP1ClientConnection(HTTP2Configuration config, SSLSession sslSession, Session tcpSession,
			ResponseHandler responseHandler) {
		super(config, sslSession, tcpSession, null, responseHandler);
		wrap = (ResponseHandlerWrap) responseHandler;
		wrap.connection = this;
	}

	@Override
	protected HttpParser initHttpParser(HTTP2Configuration config, RequestHandler requestHandler,
			ResponseHandler responseHandler) {
		return new HttpParser(responseHandler, config.getMaxRequestHeadLength());
	}

	@Override
	protected HttpGenerator initHttpGenerator() {
		return new HttpGenerator();
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

	HTTP2Configuration getHTTP2Configuration() {
		return config;
	}

	Session getTcpSession() {
		return tcpSession;
	}

	boolean upgradeProtocolToHTTP2(MetaData.Request request, MetaData.Response response) {
		if (http2ConnectionPromise != null && http2Sessionlistener != null) {
			String upgradeValue = response.getFields().get(HttpHeader.UPGRADE);
			if (response.getStatus() == HttpStatus.SWITCHING_PROTOCOLS_101 && "h2c".equalsIgnoreCase(upgradeValue)) {
				upgradeHTTP2Successfully = true;

				// initialize http2 client connection;
				final HTTP2ClientConnection http2Connection = new HTTP2ClientConnection(getHTTP2Configuration(),
						getTcpSession(), null, http2Sessionlistener) {
					@Override
					protected HTTP2Session initHTTP2Session(HTTP2Configuration config, FlowControlStrategy flowControl,
							Listener listener) {
						return HTTP2ClientSession.initSessionForUpgradingHTTP2(scheduler, this.tcpSession, generator,
								listener, flowControl, 3, config.getStreamIdleTimeout(), initStream,
								initStreamListener);
					}
				};
				getTcpSession().attachObject(http2Connection);
				http2Connection.initialize(getHTTP2Configuration(), http2ConnectionPromise, http2Sessionlistener);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public void upgradeHTTP2WithCleartext(final MetaData.Request request, final SettingsFrame settings,
			final Promise<HTTPClientConnection> promise, final ClientHTTPHandler handler) {
		upgradeHTTP2WithCleartext(request, settings,
				promise, new HTTP2ClientResponseHandler.ClientStreamPromise(request,
						new Promise.Adapter<HTTPOutputStream>(), true),
				new HTTP2ClientResponseHandler(request, handler, this), new Listener.Adapter() {

					@Override
					public Map<Integer, Integer> onPreface(com.firefly.codec.http2.stream.Session session) {
						return settings.getSettings();
					}

					@Override
					public void onFailure(com.firefly.codec.http2.stream.Session session, Throwable failure) {
						log.error("client failure, {}", failure, session);
					}

				}, handler);
	}

	public void upgradeHTTP2WithCleartext(MetaData.Request request, SettingsFrame settings,
			final Promise<HTTPClientConnection> promise, final Promise<Stream> initStream,
			final Stream.Listener initStreamListener, final Listener listener, final ClientHTTPHandler handler) {
		if (isEncrypted()) {
			throw new IllegalStateException("The TLS TCP connection must use ALPN to upgrade HTTP2");
		}

		this.http2ConnectionPromise = promise;
		this.http2Sessionlistener = listener;
		this.initStream = initStream;
		this.initStreamListener = initStreamListener;

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
					byte[] settingsFrame = out.toByteArray();
					byte[] settingsPayload = new byte[settingsFrame.length - 9];
					System.arraycopy(settingsFrame, 9, settingsPayload, 0, settingsPayload.length);

					request.getFields().add(new HttpField(HttpHeader.HTTP2_SETTINGS,
							Base64Utils.encodeToUrlSafeString(settingsPayload)));
				} catch (IOException e) {
					log.error("generate http2 upgrading settings exception", e);
				}
			} else {
				request.getFields().add(new HttpField(HttpHeader.HTTP2_SETTINGS, ""));
			}
		} else {
			request.getFields().add(new HttpField(HttpHeader.HTTP2_SETTINGS, ""));
		}

		request(request, handler);
	}

	@Override
	public HTTPOutputStream requestWithContinuation(MetaData.Request request, ClientHTTPHandler handler) {
		request.getFields().put(HttpHeader.EXPECT, HttpHeaderValue.CONTINUE);
		HTTPOutputStream outputStream = requestWithStream(request, handler);
		try {
			outputStream.commit();
		} catch (IOException e) {
			generator.reset();
			log.error("client generates the HTTP message exception", e);
		}
		return outputStream;
	}

	@Override
	public void request(MetaData.Request request, ClientHTTPHandler handler) {
		try (HTTPOutputStream output = requestWithStream(request, handler)) {
			log.debug("client request and does not send data");
		} catch (IOException e) {
			generator.reset();
			log.error("client generates the HTTP message exception", e);
		}
	}

	@Override
	public void request(MetaData.Request request, ByteBuffer buffer, ClientHTTPHandler handler) {
		try (HTTPOutputStream output = requestWithStream(request, handler)) {
			if (buffer != null) {
				output.writeWithContentLength(buffer);
			}
		} catch (IOException e) {
			generator.reset();
			log.error("client generates the HTTP message exception", e);
		}
	}

	@Override
	public void request(MetaData.Request request, ByteBuffer[] buffers, ClientHTTPHandler handler) {
		try (HTTPOutputStream output = requestWithStream(request, handler)) {
			if (buffers != null) {
				output.writeWithContentLength(buffers);
			}
		} catch (IOException e) {
			generator.reset();
			log.error("client generates the HTTP message exception", e);
		}
	}

	@Override
	public HTTPOutputStream requestWithStream(MetaData.Request request, ClientHTTPHandler handler) {
		HTTP1ClientResponseHandler http1ClientResponseHandler = new HTTP1ClientResponseHandler(handler);
		checkWrite(request, http1ClientResponseHandler);
		http1ClientResponseHandler.outputStream = new HTTP1ClientRequestOutputStream(this, wrap.writing.get().request);
		return http1ClientResponseHandler.outputStream;
	}

	@Override
	public void requestWithStream(Request request, Promise<HTTPOutputStream> promise, ClientHTTPHandler handler) {
		promise.succeeded(requestWithStream(request, handler));
	}

	static class HTTP1ClientRequestOutputStream extends AbstractHTTP1OutputStream {

		private final HTTP1ClientConnection connection;

		private HTTP1ClientRequestOutputStream(HTTP1ClientConnection connection, MetaData.Request request) {
			super(request, true);
			this.connection = connection;
		}

		@Override
		protected void generateHTTPMessageSuccessfully() {
			log.debug("client session {} generates the HTTP message completely", connection.tcpSession.getSessionId());
			connection.generator.reset();
		}

		@Override
		protected void generateHTTPMessageExceptionally(HttpGenerator.Result generatorResult,
				HttpGenerator.State generatorState) {
			if (log.isDebugEnabled()) {
				log.debug("http1 generator error, the result is {}, and the generator state is {}", generatorResult,
						generatorState);
			}
			connection.getGenerator().reset();
			throw new IllegalStateException("client generates http message exception.");
		}

		@Override
		protected ByteBuffer getHeaderByteBuffer() {
			return BufferUtils.allocate(connection.getHTTP2Configuration().getMaxRequestHeadLength());
		}

		@Override
		protected Session getSession() {
			return connection.getTcpSession();
		}

		@Override
		protected HttpGenerator getHttpGenerator() {
			return connection.getGenerator();
		}
	}

	private void checkWrite(MetaData.Request request, HTTP1ClientResponseHandler handler) {
		if (request == null)
			throw new IllegalArgumentException("the http client request is null");

		if (handler == null)
			throw new IllegalArgumentException("the http1 client response handler is null");

		if (!isOpen())
			throw new IllegalStateException("current client session " + tcpSession.getSessionId() + " has been closed");

		if (upgradeHTTP2Successfully)
			throw new IllegalStateException(
					"current client session " + tcpSession.getSessionId() + " has upgraded HTTP2");

		if (wrap.writing.compareAndSet(null, handler)) {
			request.getFields().put(HttpHeader.HOST, tcpSession.getRemoteAddress().getHostString());
			handler.connection = this;
			handler.request = request;
		} else {
			throw new WritePendingException();
		}
	}

}
