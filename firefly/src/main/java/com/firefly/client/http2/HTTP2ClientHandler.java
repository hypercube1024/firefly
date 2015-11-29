package com.firefly.client.http2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.alpn.ALPN;

import com.firefly.codec.common.Callback;
import com.firefly.codec.http2.frame.PrefaceFrame;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.frame.WindowUpdateFrame;
import com.firefly.codec.http2.stream.FlowControlStrategy;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.SessionSPI;
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
	private final List<String> protocols = Arrays.asList("http/1.1", "h2", "h2-17", "h2-16", "h2-15", "h2-14");
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
			new SSLSession(sslContext, sslEngine, session, true, new SSLEventHandler() {

				@Override
				public void handshakeFinished(SSLSession sslSession) {
					if (context.serverSelectedProtocol.equals("http/1.1")) {
						// TODO support http 1.1
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
					if (protocols.contains(protocol)) {
						// TODO select decoder
						if (protocol.equals("http/1.1")) {

						} else {

						}
						ALPN.remove(sslEngine);
					} else {
						log.info("Could not negotiate protocol: server [{}] - client {}", protocol, protocols);
						ALPN.remove(sslEngine);
						session.close(false);
					}
				}
			});
		} else {
			// TODO negotiate protocol without ALPN

			initializeHTTP2ClientConnection(session, context, null);
		}
	}

	private void initializeHTTP2ClientConnection(final Session session, final HTTP2ClientContext context,
			final SSLSession sslSession) {
		final HTTP2ClientConnection connection = new HTTP2ClientConnection(config, session, sslSession,
				context.listener);
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
	}

	@Override
	public void sessionClosed(Session session) throws Throwable {
		try {
			HTTP2ClientConnection http2ClientConnection = (HTTP2ClientConnection) session.getAttachment();
			if (http2ClientConnection != null && http2ClientConnection.isOpen()) {
				http2ClientConnection.close();
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
		log.error("client handling exception", t);
		HTTP2ClientConnection http2ClientConnection = (HTTP2ClientConnection) session.getAttachment();
		if (http2ClientConnection != null && http2ClientConnection.isOpen()) {
			http2ClientConnection.close();
		}
	}

}
