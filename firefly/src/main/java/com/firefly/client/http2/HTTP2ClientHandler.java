package com.firefly.client.http2;

import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.alpn.ALPN;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.AbstractHTTPHandler;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLEventHandler;
import com.firefly.net.tcp.ssl.SSLSession;

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
					switch (context.httpVersion) {
					case HTTP_1_1:
						initializeHTTP1ClientConnection(session, context, (SSLSession) session.getAttachment());
						break;
					case HTTP_2:
						initializeHTTP2ClientConnection(session, context, (SSLSession) session.getAttachment());
						break;
					default:
						throw new IllegalStateException(
								"client does not support the http version " + context.httpVersion);
					}
				}
			}, new ALPN.ClientProvider() {

				@Override
				public List<String> protocols() {
					return protocols;
				}

				@Override
				public void unsupported() {
					context.httpVersion = HttpVersion.HTTP_1_1;
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
			HTTP1ClientConnection http1ClientConnection = new HTTP1ClientConnection(config, session, sslSession);
			session.attachObject(http1ClientConnection);
			context.promise.succeeded(http1ClientConnection);
		} catch (Throwable t) {
			context.promise.failed(t);
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
			connection.initialize(config, context.promise, context.listener);
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
