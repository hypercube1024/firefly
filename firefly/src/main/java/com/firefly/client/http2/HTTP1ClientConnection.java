package com.firefly.client.http2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.AbstractHTTP1Connection;
import com.firefly.codec.http2.stream.AbstractHTTP1OutputStream;
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

	private static final Log log = LogFactory.getInstance().getLog("firefly-system");

	private final ResponseHandlerWrap wrap;
	private volatile HTTPClientRequest request;
	volatile boolean upgradeHTTP2Successfully = false;

	private static class ResponseHandlerWrap implements ResponseHandler {

		private final AtomicReference<HTTP1ClientResponseHandler> writing = new AtomicReference<>();

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
			return writing.getAndSet(null).messageComplete();
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

	public HTTPClientRequest getRequest() {
		return request;
	}

	void reset() {
		request = null;
		generator.reset();
		parser.reset();
	}

	void initializeHTTP2ClientConnection(final Promise<HTTPConnection> promise, final Listener listener) {
		// initialize http2 client connection;
		final HTTP2ClientConnection http2Connection = new HTTP2ClientConnection(config, tcpSession, null, listener);
		tcpSession.attachObject(http2Connection);
		http2Connection.initialize(config, promise, listener);
	}

	public void upgradeHTTP2WithCleartext(HTTPClientRequest request, SettingsFrame settings,
			final Promise<HTTPConnection> promise, final Listener listener, final HTTP1ClientResponseHandler handler) {
		if (isEncrypted()) {
			throw new IllegalStateException("The TLS TCP connection must use ALPN to upgrade HTTP2");
		}

		handler.promise = promise;
		handler.listener = listener;

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

		request(request, handler);
	}

	public void request(HTTPClientRequest request, HTTP1ClientResponseHandler handler) {
		try (HTTP1ClientRequestOutputStream output = requestWithStream(request, handler)) {
			log.debug("client request and does not send data");
		} catch (IOException e) {
			generator.reset();
			log.error("client generates the HTTP message exception", e);
		}
	}

	public void request(HTTPClientRequest request, ByteBuffer data, HTTP1ClientResponseHandler handler) {
		try (HTTP1ClientRequestOutputStream output = requestWithStream(request, handler)) {
			if (data != null) {
				output.writeAndClose(data);
			}
		} catch (IOException e) {
			generator.reset();
			log.error("client generates the HTTP message exception", e);
		}
	}

	public void request(HTTPClientRequest request, ByteBuffer[] dataArray, HTTP1ClientResponseHandler handler) {
		try (HTTP1ClientRequestOutputStream output = requestWithStream(request, handler)) {
			if (dataArray != null) {
				for (ByteBuffer data : dataArray) {
					output.write(data);
				}
			}
		} catch (IOException e) {
			generator.reset();
			log.error("client generates the HTTP message exception", e);
		}
	}

	public HTTP1ClientRequestOutputStream requestWithStream(HTTPClientRequest request,
			HTTP1ClientResponseHandler handler) {
		checkWrite(request, handler);
		HTTP1ClientRequestOutputStream outputStream = new HTTP1ClientRequestOutputStream(this, request);
		return outputStream;
	}

	public static class HTTP1ClientRequestOutputStream extends AbstractHTTP1OutputStream {

		private final HTTP1ClientConnection connection;

		private HTTP1ClientRequestOutputStream(HTTP1ClientConnection connection, HTTPClientRequest request) {
			super(request, true);
			this.connection = connection;
		}

		@Override
		protected void generateHTTPMessageSuccessfully() {
			log.debug("client session {} generates the HTTP message completely", connection.tcpSession.getSessionId());
		}

		@Override
		protected void generateHTTPMessageExceptionally() {
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

	private void checkWrite(HTTPClientRequest request, HTTP1ClientResponseHandler handler) {
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
			handler.connection = this;
			this.request = request;
		} else {
			throw new WritePendingException();
		}
	}

}
