package com.firefly.client.http2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.PrefaceFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.frame.WindowUpdateFrame;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpHeaderValue;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.stream.AbstractHTTP2Connection;
import com.firefly.codec.http2.stream.FlowControlStrategy;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTP2Session;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.codec.http2.stream.SessionSPI;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ClientConnection extends AbstractHTTP2Connection implements HTTPClientConnection {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public void initialize(HTTP2Configuration config, final Promise<HTTPClientConnection> promise,
			final Listener listener) {
		Map<Integer, Integer> settings = listener.onPreface(getHttp2Session());
		if (settings == null) {
			settings = Collections.emptyMap();
		}
		PrefaceFrame prefaceFrame = new PrefaceFrame();
		SettingsFrame settingsFrame = new SettingsFrame(settings, false);
		SessionSPI sessionSPI = getSessionSPI();
		int windowDelta = config.getInitialSessionRecvWindow() - FlowControlStrategy.DEFAULT_WINDOW_SIZE;
		Callback callback = new Callback() {

			@Override
			public void succeeded() {
				promise.succeeded(HTTP2ClientConnection.this);
			}

			@Override
			public void failed(Throwable x) {
				try {
					HTTP2ClientConnection.this.close();
				} catch (IOException e) {
					log.error("http2 client connection initialization error", e);
				}
				promise.failed(x);
			}
		};

		if (windowDelta > 0) {
			sessionSPI.updateRecvWindow(windowDelta);
			sessionSPI.frames(null, callback, prefaceFrame, settingsFrame, new WindowUpdateFrame(0, windowDelta));
		} else {
			sessionSPI.frames(null, callback, prefaceFrame, settingsFrame);
		}
	}

	public HTTP2ClientConnection(HTTP2Configuration config, Session tcpSession, SSLSession sslSession,
			Listener listener) {
		super(config, tcpSession, sslSession, listener);
	}

	@Override
	protected HTTP2Session initHTTP2Session(HTTP2Configuration config, FlowControlStrategy flowControl,
			Listener listener) {
		HTTP2ClientSession http2ClientSession = new HTTP2ClientSession(scheduler, this.tcpSession, this.generator,
				listener, flowControl, config.getStreamIdleTimeout());
		return http2ClientSession;
	}

	@Override
	protected Parser initParser(HTTP2Configuration config) {
		return new Parser((HTTP2ClientSession) http2Session, config.getMaxDynamicTableSize(),
				config.getMaxRequestHeadLength());
	}

	Parser getParser() {
		return parser;
	}

	Generator getGenerator() {
		return generator;
	}

	SSLSession getSSLSession() {
		return sslSession;
	}

	SessionSPI getSessionSPI() {
		return http2Session;
	}

	@Override
	public void send(Request request, ClientHTTPHandler handler) {
		Promise<HTTPOutputStream> promise = new Promise<HTTPOutputStream>() {

			@Override
			public void succeeded(HTTPOutputStream output) {
				try {
					output.close();
				} catch (IOException e) {
					log.error("write data unsuccessfully", e);
				}

			}

			@Override
			public void failed(Throwable x) {
				log.error("write data unsuccessfully", x);
			}
		};

		request(request, true, promise, handler);
	}

	@Override
	public void send(Request request, final ByteBuffer buffer, ClientHTTPHandler handler) {
		request.getFields().put(HttpHeader.CONTENT_LENGTH, String.valueOf(buffer.remaining()));

		Promise<HTTPOutputStream> promise = new Promise<HTTPOutputStream>() {

			@Override
			public void succeeded(HTTPOutputStream output) {
				try {
					output.writeWithContentLength(buffer);
				} catch (IOException e) {
					log.error("write data unsuccessfully", e);
				}

			}

			@Override
			public void failed(Throwable x) {
				log.error("write data unsuccessfully", x);
			}
		};

		send(request, promise, handler);
	}

	@Override
	public void send(Request request, final ByteBuffer[] buffers, ClientHTTPHandler handler) {
		long contentLength = 0;
		for (ByteBuffer buf : buffers) {
			contentLength += buf.remaining();
		}
		request.getFields().put(HttpHeader.CONTENT_LENGTH, String.valueOf(contentLength));

		Promise<HTTPOutputStream> promise = new Promise<HTTPOutputStream>() {

			@Override
			public void succeeded(HTTPOutputStream output) {
				try {
					output.writeWithContentLength(buffers);
				} catch (IOException e) {
					log.error("write data unsuccessfully", e);
				}

			}

			@Override
			public void failed(Throwable x) {
				log.error("write data unsuccessfully", x);
			}
		};

		send(request, promise, handler);
	}

	@Override
	public HTTPOutputStream sendRequestWithContinuation(MetaData.Request request, ClientHTTPHandler handler) {
		request.getFields().put(HttpHeader.EXPECT, HttpHeaderValue.CONTINUE);
		return getHTTPOutputStream(request, handler);
	}

	@Override
	public HTTPOutputStream getHTTPOutputStream(final Request request, final ClientHTTPHandler handler) {
		FuturePromise<HTTPOutputStream> promise = new FuturePromise<>();
		send(request, promise, handler);
		try {
			return promise.get();
		} catch (Throwable e) {
			log.error("get http output stream unsuccessfully", e);
			return null;
		}
	}

	@Override
	public void send(final Request request, final Promise<HTTPOutputStream> promise, final ClientHTTPHandler handler) {
		request(request, false, promise, handler);
	}

	public void request(final Request request, boolean endStream, final Promise<HTTPOutputStream> promise,
			final ClientHTTPHandler handler) {
		http2Session.newStream(new HeadersFrame(request, null, endStream),
				new HTTP2ClientResponseHandler.ClientStreamPromise(request, promise, endStream),
				new HTTP2ClientResponseHandler(request, handler, this));
	}

	@Override
	public void upgradeHTTP2(Request request, SettingsFrame settings, Promise<HTTPClientConnection> promise,
			ClientHTTPHandler handler) {
		new RuntimeException("current connection version is http2, it does not need to upgrading");
	}

}
