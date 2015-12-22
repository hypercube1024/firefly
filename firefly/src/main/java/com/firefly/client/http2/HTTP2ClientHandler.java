package com.firefly.client.http2;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.alpn.ALPN;

import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLContextFactory;
import com.firefly.net.tcp.ssl.SSLEventHandler;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HTTP2ClientHandler implements Handler {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final HTTP2Configuration config;
	private final Map<Integer, HTTP2ClientContext> http2ClientContext;
	private final List<String> protocols = Arrays.asList("h2", "h2-17", "h2-16", "h2-15", "h2-14", "http/1.1");
	private SSLContext sslContext;

	public HTTP2ClientHandler(HTTP2Configuration config, Map<Integer, HTTP2ClientContext> http2ClientContext) {
		this.config = config;
		this.http2ClientContext = http2ClientContext;

		try {
			if (config.isSecure()) {
				if (VerifyUtils.isNotEmpty(config.getCredentialPath())
						&& VerifyUtils.isNotEmpty(config.getKeyPassword())
						&& VerifyUtils.isNotEmpty(config.getKeystorePassword())) {
					FileInputStream in = new FileInputStream(new File(config.getCredentialPath()));
					sslContext = SSLContextFactory.getSSLContext(in, config.getKeystorePassword(),
							config.getKeyPassword());
				} else {
					sslContext = SSLContextFactory.getSSLContext();
				}
			}
		} catch (Throwable t) {
			log.error("create ssl context error", t);
		}
	}

	@Override
	@SuppressWarnings("resource")
	public void sessionOpened(final Session session) throws Throwable {
		final HTTP2ClientContext context = http2ClientContext.get(session.getSessionId());
		try {
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
				new SSLSession(sslContext, sslEngine, session, true, new SSLEventHandler() {

					@Override
					public void handshakeFinished(SSLSession sslSession) {
						if (context.serverSelectedProtocol.equals("http/1.1")) {
							initializeHTTP1ClientConnection(session, context, sslSession);
						} else {
							initializeHTTP2ClientConnection(session, context, sslSession);
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
								context.serverSelectedProtocol = protocol;
							} else {
								log.info("The client can not negotiate protocol. server [{}] - client {}", protocol,
										protocols);
								session.close();
							}
						} finally {
							ALPN.remove(sslEngine);
						}
					}
				});
			} else {
				// TODO negotiate protocol without ALPN
				initializeHTTP2ClientConnection(session, context, null);
			}
		} finally {
			http2ClientContext.remove(session.getSessionId());
		}
	}

	private void initializeHTTP1ClientConnection(final Session session, final HTTP2ClientContext context,
			final SSLSession sslSession) {
		// TODO not implements
	}

	private void initializeHTTP2ClientConnection(final Session session, final HTTP2ClientContext context,
			final SSLSession sslSession) {
		HTTP2ClientConnection.initialize(config, session, context, sslSession);
	}

	@Override
	public void sessionClosed(Session session) throws Throwable {
		log.info("client session {} closed", session.getSessionId());
		try {
			HTTPConnection httpConnection = (HTTPConnection) session.getAttachment();
			if (httpConnection != null && httpConnection.isOpen()) {
				httpConnection.close();
			}
		} catch (Throwable t) {
			log.error("http2 conection close exception", t);
		} finally {
			http2ClientContext.remove(session.getSessionId());
		}
	}

	@Override
	public void messageRecieved(Session session, Object message) throws Throwable {
	}

	@Override
	public void exceptionCaught(Session session, Throwable t) throws Throwable {
		log.error("client handling exception", t);
		HTTPConnection httpConnection = (HTTPConnection) session.getAttachment();
		if (httpConnection != null && httpConnection.isOpen()) {
			httpConnection.close();
		}
	}

}
