package com.firefly.net.tcp;

import java.nio.ByteBuffer;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.alpn.ALPN;

import com.firefly.net.Handler;
import com.firefly.net.SSLEventHandler;
import com.firefly.net.Session;
import com.firefly.net.tcp.aio.AsynchronousTcpServer;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;
import com.firefly.utils.function.Func1;
import com.firefly.utils.lang.AbstractLifeCycle;

public class SimpleTcpServer extends AbstractLifeCycle {

	// TCP connection callback
	protected Action1<TcpConnection> acceptConnection;
	protected Action2<Integer, Throwable> connectionFailed;

	// SSL connection callback
	protected Action1<SSLSession> sslHandshakeFinished;
	protected Action0 alpnUnsupported;
	protected Func1<List<String>, String> alpnSelectProtocols;

	protected AsynchronousTcpServer server;
	protected TcpServerConfiguration config;

	public SimpleTcpServer() {
		this(new TcpServerConfiguration());
	}

	public SimpleTcpServer(TcpServerConfiguration config) {
		this.config = config;
		server = new AsynchronousTcpServer(config);
	}

	public SimpleTcpServer accept(Action1<TcpConnection> acceptConnection) {
		this.acceptConnection = acceptConnection;
		return this;
	}

	public SimpleTcpServer connectionFailed(Action2<Integer, Throwable> connectionFailed) {
		this.connectionFailed = connectionFailed;
		return this;
	}

	public void listen(String host, int port) {
		config.setHost(host);
		config.setPort(port);
		start();
	}

	@Override
	protected void init() {
		if (config.isSecureConnectionEnabled() == false) {
			config.setDecoder((ByteBuffer buf, Session session) -> {
				Object o = session.getAttachment();
				if (o != null) {
					TcpConnectionImpl c = (TcpConnectionImpl) o;
					if (c.buffer != null) {
						c.buffer.call(buf);
					}
				}
			});
			config.setEncoder((Object message, Session session) -> {
			});
			config.setHandler(new AbstractServerHandler() {

				@Override
				public void sessionOpened(Session session) throws Throwable {
					TcpConnectionImpl connection = new TcpConnectionImpl(session);
					session.attachObject(connection);
					if (acceptConnection != null) {
						acceptConnection.call(connection);
					}
				}

			});
		} else {
			config.setDecoder((ByteBuffer buf, Session session) -> {
				Object o = session.getAttachment();
				if (o != null) {
					SecureTcpConnectionImpl c = (SecureTcpConnectionImpl) o;
					ByteBuffer plaintext = c.sslSession.read(buf);
					if (plaintext != null) {
						if (c.buffer != null) {
							c.buffer.call(buf);
						}
					}
				}
			});
			config.setEncoder((Object message, Session session) -> {
			});

			config.setHandler(new AbstractServerHandler() {

				private SSLContext sslContext = config.getSslContextFactory().getSSLContext();

				@Override
				public void sessionOpened(Session session) throws Throwable {

					final SSLEngine sslEngine = sslContext.createSSLEngine();
					SSLSession sslSession = new SSLSession(sslContext, sslEngine, session, false,
							new SSLEventHandler() {

								@Override
								public void handshakeFinished(SSLSession session) {
									sslHandshakeFinished.call(session);
								}
							}, new ALPN.ServerProvider() {

								@Override
								public void unsupported() {
									try {
										alpnUnsupported.call();
									} finally {
										ALPN.remove(sslEngine);
									}
								}

								@Override
								public String select(List<String> protocols) {
									try {
										return alpnSelectProtocols.call(protocols);
									} finally {
										ALPN.remove(sslEngine);
									}
								}
							});
					SecureTcpConnectionImpl connection = new SecureTcpConnectionImpl(session, sslSession);
					session.attachObject(connection);
					if (acceptConnection != null) {
						acceptConnection.call(connection);
					}
				}

			});
		}
		server.listen(config.getHost(), config.getPort());
	}

	abstract private class AbstractServerHandler implements Handler {

		@Override
		public void sessionClosed(Session session) throws Throwable {
			Object o = session.getAttachment();
			if (o != null) {
				AbstractTcpConnection c = (AbstractTcpConnection) o;
				if (c.closeCallback != null) {
					c.closeCallback.call();
				}
			}
		}

		@Override
		public void messageRecieved(Session session, Object message) throws Throwable {
		}

		@Override
		public void exceptionCaught(Session session, Throwable t) throws Throwable {
			Object o = session.getAttachment();
			if (o != null) {
				AbstractTcpConnection c = (AbstractTcpConnection) o;
				if (c.exception != null) {
					c.exception.call(t);
				}
			}

		}

		@Override
		public void failedAcceptingSession(Integer sessionId, Throwable t) throws Throwable {
			if (connectionFailed != null) {
				connectionFailed.call(sessionId, t);
			}
		}

	}

	@Override
	protected void destroy() {
		server.stop();
	}

}
