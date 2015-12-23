package com.firefly.client.http2;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.alpn.ALPN;

import com.firefly.codec.http2.frame.PrefaceFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.frame.WindowUpdateFrame;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.AbstractHTTPHandler;
import com.firefly.codec.http2.stream.FlowControlStrategy;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.SessionSPI;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLEventHandler;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.concurrent.Callback;

public class HTTP2ClientHandler extends AbstractHTTPHandler {

	private final Map<Integer, HTTP2ClientContext> http2ClientContext;

	public HTTP2ClientHandler(HTTP2Configuration config, Map<Integer, HTTP2ClientContext> http2ClientContext) {
		super(config);
		this.http2ClientContext = http2ClientContext;
	}

	@Override
	public void sessionOpened(final Session session) throws Throwable {
		final HTTP2ClientContext context = http2ClientContext.get(session.getSessionId());

		if (context == null) {
			log.error("http2 client can not get the client context of session {}", session.getSessionId());
			session.closeNow();
			return;
		}

		if (config.isSecure()) {
			if (sslContext == null) {
				context.promise.failed(new IllegalStateException("the ssl context is null"));
				return;
			}

			final SSLEngine sslEngine = sslContext.createSSLEngine();
			session.attachObject(new SSLSession(sslContext, sslEngine, session, true, new SSLEventHandler() {

				@Override
				public void handshakeFinished(SSLSession sslSession) {
					log.debug("client session {} SSL handshake finished", session.getSessionId());
					if (context.httpVersion == HttpVersion.HTTP_1_1) {
						initializeHTTP1ClientConnection(session, context, (SSLSession) session.getAttachment());
					} else {
						initializeHTTP2ClientConnection(session, context, (SSLSession) session.getAttachment());
					}
				}
			}, new ALPN.ClientProvider() {

				@Override
				public List<String> protocols() {
					return protocols;
				}

				@Override
				public void unsupported() {
					ALPN.remove(sslEngine);
				}

				@Override
				public void selected(String protocol) {
					try {
						if (protocols.contains(protocol)) {
							log.debug("HTTP2 client selected protocol {}", protocol);
							if ("http/1.1".equalsIgnoreCase(protocol)) {
								context.httpVersion = HttpVersion.HTTP_1_1;
							} else {
								context.httpVersion = HttpVersion.HTTP_2;
							}
						} else {
							log.info("The client can not negotiate protocol. server [{}] - client {}", protocol,
									protocols);
							session.close();
						}
					} finally {
						ALPN.remove(sslEngine);
					}
				}
			}));
		} else {
			initializeHTTP1ClientConnection(session, context, null);
		}
	}

	private void initializeHTTP1ClientConnection(final Session session, final HTTP2ClientContext context,
			final SSLSession sslSession) {
		try {
			session.attachObject(new HTTP1ClientConnection(config, session, sslSession));
		} finally {
			http2ClientContext.remove(session.getSessionId());
		}
	}

	private void initializeHTTP2ClientConnection(final Session session, final HTTP2ClientContext context,
			final SSLSession sslSession) {
		try {
			final HTTP2ClientConnection connection = new HTTP2ClientConnection(config, session, sslSession,
					context.listener);
			session.attachObject(connection);

			Map<Integer, Integer> settings = context.listener.onPreface(connection.getHttp2Session());
			if (settings == null) {
				settings = Collections.emptyMap();
			}
			PrefaceFrame prefaceFrame = new PrefaceFrame();
			SettingsFrame settingsFrame = new SettingsFrame(settings, false);
			SessionSPI sessionSPI = connection.getSessionSPI();
			int windowDelta = config.getInitialSessionRecvWindow() - FlowControlStrategy.DEFAULT_WINDOW_SIZE;
			Callback callback = new Callback() {

				@Override
				public void succeeded() {
					context.promise.succeeded(connection);
				}

				@Override
				public void failed(Throwable x) {
					try {
						connection.close();
					} catch (IOException e) {
						log.error("http2 connection initialization error", e);
					}
					context.promise.failed(x);
				}
			};

			if (windowDelta > 0) {
				sessionSPI.updateRecvWindow(windowDelta);
				sessionSPI.frames(null, callback, prefaceFrame, settingsFrame, new WindowUpdateFrame(0, windowDelta));
			} else {
				sessionSPI.frames(null, callback, prefaceFrame, settingsFrame);
			}

		} finally {
			http2ClientContext.remove(session.getSessionId());
		}
	}

	@Override
	public void sessionClosed(Session session) throws Throwable {
		try {
			super.sessionClosed(session);
		} finally {
			http2ClientContext.remove(session.getSessionId());
		}
	}

}
