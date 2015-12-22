package com.firefly.server.http2;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.alpn.ALPN;

import com.firefly.codec.http2.model.HttpVersion;
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

public class HTTP2ServerHandler implements Handler {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final HTTP2Configuration config;
	private final ServerSessionListener listener;
	private final List<String> serverProtocols = Arrays.asList("h2", "h2-17", "h2-16", "h2-15", "h2-14", "http/1.1");
	private final String defaultProtocol = "http/1.1";
	private SSLContext sslContext;

	public HTTP2ServerHandler(HTTP2Configuration config, ServerSessionListener listener) {
		this.config = config;
		this.listener = listener;

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

	@SuppressWarnings("resource")
	@Override
	public void sessionOpened(final Session session) throws Throwable {
		if (config.isSecure()) {
			final SSLEngine sslEngine = sslContext.createSSLEngine();
			new SSLSession(sslContext, sslEngine, session, false, new SSLEventHandler() {

				@Override
				public void handshakeFinished(SSLSession sslSession) {
					if (session.getAttachment() instanceof HttpVersion) {
						HttpVersion httpVersion = (HttpVersion) session.getAttachment();
						if (httpVersion == HttpVersion.HTTP_2) {
							session.attachObject(new HTTP2ServerConnection(config, session, sslSession, listener));
						} else {
							// TODO initialize HTTP 1.1 server connection
						}
					} else {
						log.error("HTTP2 server can not get the HTTP version of session {}", session.getSessionId());
						session.closeNow();
					}
				}
			}, new ALPN.ServerProvider() {

				@Override
				public void unsupported() {
					ALPN.remove(sslEngine);
				}

				@Override
				public String select(List<String> clientProtocols) {
					try {
						for (String clientProtocol : clientProtocols) {
							for (String serverProtocol : serverProtocols) {
								if (serverProtocol.equals(clientProtocol)) {
									if (serverProtocol.equals("http/1.1")) {
										session.attachObject(HttpVersion.HTTP_1_1);
									} else {
										session.attachObject(HttpVersion.HTTP_2);
									}
									return clientProtocol;
								}
							}
						}
						return defaultProtocol;
					} finally {
						ALPN.remove(sslEngine);
					}
				}
			});
		} else {
			// TODO negotiate protocol without ALPN
			session.attachObject(new HTTP2ServerConnection(config, session, null, listener));
		}
	}

	@Override
	public void sessionClosed(Session session) throws Throwable {
		log.info("server session {} closed", session.getSessionId());
		try {
			HTTPConnection httpConnection = (HTTPConnection) session.getAttachment();
			if (httpConnection != null && httpConnection.isOpen()) {
				httpConnection.close();
			}
		} catch (Throwable t) {
			log.error("http2 conection close exception", t);
		}
	}

	@Override
	public void messageRecieved(Session session, Object message) throws Throwable {

	}

	@Override
	public void exceptionCaught(Session session, Throwable t) throws Throwable {
		log.error("server handling exception", t);
		HTTPConnection httpConnection = (HTTPConnection) session.getAttachment();
		if (httpConnection != null && httpConnection.isOpen()) {
			httpConnection.close();
		}
	}

}
