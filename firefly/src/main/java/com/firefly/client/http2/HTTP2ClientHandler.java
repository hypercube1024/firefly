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
	private final List<String> protocols = Arrays.asList("h2", "h2-17", "h2-16", "h2-15", "h2-14");
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
	public void sessionOpened(Session session) throws Throwable {
		HTTP2ClientContext context = http2ClientContext.get(session.getSessionId());
		if (context == null) {
			log.error("http2 client can not get the client context of session {}", session.getSessionId());
			session.close(true);
			return;
		}

		if (config.isSecure()) {
			if (sslContext == null) {
				context.promise.failed(new IllegalStateException("the ssl context is null"));
				return;
			}
			final SSLEngine sslEngine = sslContext.createSSLEngine();
			final SSLSession sslSession = new SSLSession(sslContext, sslEngine, session, true, new SSLEventHandler() {

				@Override
				public void handshakeFinished(SSLSession sslSession) {
					// TODO
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

					if (protocols.contains(protocol)) {
						ALPN.remove(sslEngine);
					} else {
						log.info("Could not negotiate protocol: server [{}] - client {}", protocol, protocols);
						// TODO
					}

				}
			});
			// TODO create http2 connection
		} else {
			new HTTP2ClientConnection(config, session, null, context.listener);
		}
	}

	@Override
	public void sessionClosed(Session session) throws Throwable {
		HTTP2ClientConnection attachment = (HTTP2ClientConnection) session.getAttachment();
		if (attachment != null) {
			attachment.close();
		}
	}

	@Override
	public void messageRecieved(Session session, Object message) throws Throwable {
		// TODO Auto-generated method stub

	}

	@Override
	public void exceptionCaught(Session session, Throwable t) throws Throwable {
		// TODO Auto-generated method stub

	}

}
