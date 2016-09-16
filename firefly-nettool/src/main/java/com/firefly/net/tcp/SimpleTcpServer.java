package com.firefly.net.tcp;

import java.nio.ByteBuffer;

import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.net.tcp.aio.AsynchronousTcpServer;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;
import com.firefly.utils.lang.AbstractLifeCycle;

public class SimpleTcpServer extends AbstractLifeCycle {

	protected Action1<TcpConnection> acceptConnection;
	protected Action2<Integer, Throwable> connectionFailed;
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
			config.setHandler(new Handler() {

				@Override
				public void sessionOpened(Session session) throws Throwable {
					TcpConnectionImpl connection = new TcpConnectionImpl(session);
					session.attachObject(connection);
					if (acceptConnection != null) {
						acceptConnection.call(connection);
					}
				}

				@Override
				public void sessionClosed(Session session) throws Throwable {
					Object o = session.getAttachment();
					if (o != null) {
						TcpConnectionImpl c = (TcpConnectionImpl) o;
						if (c.closeCallback != null) {
							c.closeCallback.call();
						}
					}
				}

				@Override
				public void messageRecieved(Session session, Object message) throws Throwable {
					// TODO Auto-generated method stub

				}

				@Override
				public void exceptionCaught(Session session, Throwable t) throws Throwable {
					Object o = session.getAttachment();
					if (o != null) {
						TcpConnectionImpl c = (TcpConnectionImpl) o;
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

			});
		} else {

		}

		server.start();
	}

	@Override
	protected void destroy() {
		server.stop();
	}

}
