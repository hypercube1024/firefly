package com.firefly.client.http2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.codec.http2.frame.ErrorCode;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.PrefaceFrame;
import com.firefly.codec.http2.frame.ResetFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.frame.WindowUpdateFrame;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpHeaderValue;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.stream.AbstractHTTP2Connection;
import com.firefly.codec.http2.stream.AbstractHTTP2OutputStream;
import com.firefly.codec.http2.stream.FlowControlStrategy;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTP2Session;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.codec.http2.stream.SessionSPI;
import com.firefly.codec.http2.stream.Stream;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.VerifyUtils;
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
	public void request(Request request, ClientHTTPHandler handler) {
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

		requestWithStream(request, promise, handler);
	}

	@Override
	public void request(Request request, final ByteBuffer buffer, ClientHTTPHandler handler) {
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

		requestWithStream(request, promise, handler);

	}

	@Override
	public void request(Request request, final ByteBuffer[] buffers, ClientHTTPHandler handler) {
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

		requestWithStream(request, promise, handler);
	}

	@Override
	public HTTPOutputStream requestWithStream(final Request request, final ClientHTTPHandler handler) {
		FuturePromise<HTTPOutputStream> promise = new FuturePromise<>();
		requestWithStream(request, promise, handler);
		try {
			return promise.get();
		} catch (Throwable e) {
			log.error("get http output stream unsuccessfully", e);
			return null;
		}
	}

	public static class ClientStreamPromise implements Promise<Stream> {

		private final Request request;
		private final Promise<HTTPOutputStream> promise;

		public ClientStreamPromise(Request request, Promise<HTTPOutputStream> promise) {
			this.request = request;
			this.promise = promise;
		}

		@Override
		public void succeeded(final Stream stream) {
			if (log.isDebugEnabled()) {
				log.debug("create a new stream {}", stream.getId());
			}
			final AbstractHTTP2OutputStream output = new AbstractHTTP2OutputStream(request, true) {

				@Override
				protected Stream getStream() {
					return stream;
				}
			};
			stream.setAttribute("outputStream", output);
			promise.succeeded(output);
		}

		@Override
		public void failed(Throwable x) {
			promise.failed(x);
			log.error("client creates stream unsuccessfully", x);
		}

	}

	public static class ClientStreamListener extends Stream.Listener.Adapter {

		private final Request request;
		private final ClientHTTPHandler handler;
		private final HTTPClientConnection connection;

		public ClientStreamListener(Request request, ClientHTTPHandler handler, HTTPClientConnection connection) {
			this.request = request;
			this.handler = handler;
			this.connection = connection;
		}

		@Override
		public void onHeaders(final Stream stream, final HeadersFrame headersFrame) {
			if (headersFrame.getMetaData() == null) {
				throw new IllegalArgumentException("the stream " + stream.getId() + " received a null meta data");
			}

			if (headersFrame.getMetaData().isResponse()) {
				final HTTPOutputStream output = (HTTPOutputStream) stream.getAttribute("outputStream");
				final MetaData.Response response = (MetaData.Response) headersFrame.getMetaData();
				stream.setAttribute("response", response);

				handler.headerComplete(request, response, output, connection);

				if (headersFrame.isEndStream()) {
					handler.messageComplete(request, response, output, connection);
				}
			} else {
				if (headersFrame.isEndStream()) {
					final HTTPOutputStream output = (HTTPOutputStream) stream.getAttribute("outputStream");
					final MetaData.Response response = (MetaData.Response) stream.getAttribute("response");

					String trailerName = response.getFields().get(HttpHeader.TRAILER);
					if (VerifyUtils.isNotEmpty(trailerName)) {
						if (headersFrame.getMetaData().getFields().containsKey(trailerName)) {
							handler.messageComplete(request, response, output, connection);
						} else {
							throw new IllegalArgumentException(
									"the stream " + stream.getId() + " received illegal meta data");
						}
					} else {
						handler.messageComplete(request, response, output, connection);
					}
				} else {
					throw new IllegalArgumentException("the stream " + stream.getId() + " received illegal meta data");
				}
			}

		}

		@Override
		public void onData(Stream stream, DataFrame dataFrame, Callback callback) {
			final HTTPOutputStream output = (HTTPOutputStream) stream.getAttribute("outputStream");
			final MetaData.Response response = (MetaData.Response) stream.getAttribute("response");

			try {
				handler.content(dataFrame.getData(), request, response, output, connection);
				callback.succeeded();
			} catch (Throwable t) {
				callback.failed(t);
			}

			if (dataFrame.isEndStream()) {
				handler.messageComplete(request, response, output, connection);
			}
		}

		@Override
		public void onReset(Stream stream, ResetFrame frame) {
			final HTTPOutputStream output = (HTTPOutputStream) stream.getAttribute("outputStream");
			final MetaData.Response response = (MetaData.Response) stream.getAttribute("response");

			ErrorCode errorCode = ErrorCode.from(frame.getError());
			String reason = errorCode == null ? "error=" + frame.getError() : errorCode.name().toLowerCase();
			handler.badMessage(frame.getError(), reason, request, response, output, connection);
		}
	}

	@Override
	public void requestWithStream(final Request request, final Promise<HTTPOutputStream> promise,
			final ClientHTTPHandler handler) {
		if (!request.getFields().contains(HttpHeader.CONTENT_LENGTH)) {
			request.getFields().put(HttpHeader.TRAILER, AbstractHTTP2OutputStream.TRAILER_NAME);
			request.getFields().put(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED);
		}

		http2Session.newStream(new HeadersFrame(request, null, false), new ClientStreamPromise(request, promise),
				new ClientStreamListener(request, handler, this));
	}

	@Override
	public void upgradeHTTP2WithCleartext(Request request, SettingsFrame settings, Promise<HTTPClientConnection> promise,
			ClientHTTPHandler handler) {
		new RuntimeException("current connection version is http2, it does not need to upgrading");
	}

}
