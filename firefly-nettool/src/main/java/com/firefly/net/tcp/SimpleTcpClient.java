package com.firefly.net.tcp;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.alpn.ALPN;

import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.net.tcp.aio.AsynchronousTcpClient;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.concurrent.FuturePromise;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Func0;
import com.firefly.utils.lang.AbstractLifeCycle;

public class SimpleTcpClient extends AbstractLifeCycle {

	// ALPN callback
	protected Action0 alpnUnsupported;
	protected Action1<String> alpnSelected;
	protected Func0<List<String>> alpnProtocols;

	protected AsynchronousTcpClient client;
	protected TcpConfiguration config;

	protected Map<Integer, ClientContext> context = new ConcurrentHashMap<>();

	public SimpleTcpClient() {
		this(new TcpConfiguration());
	}

	public SimpleTcpClient(TcpConfiguration config) {
		client = new AsynchronousTcpClient(config);
		this.config = config;
	}

	public SimpleTcpClient alpnUnsupported(Action0 alpnUnsupported) {
		this.alpnUnsupported = alpnUnsupported;
		return this;
	}

	public SimpleTcpClient alpnSelected(Action1<String> alpnSelected) {
		this.alpnSelected = alpnSelected;
		return this;
	}

	public SimpleTcpClient alpnProtocols(Func0<List<String>> alpnProtocols) {
		this.alpnProtocols = alpnProtocols;
		return this;
	}

	public FuturePromise<TcpConnection> connect(String host, int port) {
		FuturePromise<TcpConnection> promise = new FuturePromise<>();
		connect(host, port, promise);
		return promise;
	}

	public void connect(String host, int port, Action1<TcpConnection> conn) {
		Promise<TcpConnection> promise = new Promise<TcpConnection>() {

			@Override
			public void succeeded(TcpConnection result) {
				conn.call(result);
			}
		};
		connect(host, port, promise);
	}

	public void connect(String host, int port, Action1<TcpConnection> conn, Action1<Throwable> failed) {
		Promise<TcpConnection> promise = new Promise<TcpConnection>() {

			@Override
			public void succeeded(TcpConnection result) {
				conn.call(result);
			}

			public void failed(Throwable x) {
				failed.call(x);
			}
		};
		connect(host, port, promise);
	}

	public void connect(String host, int port, Promise<TcpConnection> promise) {
		start();
		int sessionId = client.connect(host, port);
		ClientContext ctx = new ClientContext();
		ctx.promise = promise;
		context.put(sessionId, ctx);
	}

	public class ClientContext {
		Promise<TcpConnection> promise;
		TcpConnection connection;
	}

	public abstract class AbstractHandler implements Handler {
		@Override
		public void messageRecieved(Session session, Object message) throws Throwable {
		}

		@Override
		public void failedOpeningSession(Integer sessionId, Throwable t) throws Throwable {
			try {
				ClientContext ctx = context.get(sessionId);
				if (ctx != null && ctx.promise != null) {
					ctx.promise.failed(t);
				}
			} finally {
				context.remove(sessionId);
			}
		}

		@Override
		public void sessionClosed(Session session) throws Throwable {
			try {
				Object o = session.getAttachment();
				if (o != null && o instanceof AbstractTcpConnection) {
					AbstractTcpConnection c = (AbstractTcpConnection) o;
					if (c.closeCallback != null) {
						c.closeCallback.call();
					}
				}
			} finally {
				context.remove(session.getSessionId());
			}
		}

		@Override
		public void exceptionCaught(Session session, Throwable t) throws Throwable {
			try {
				Object o = session.getAttachment();
				if (o != null && o instanceof AbstractTcpConnection) {
					AbstractTcpConnection c = (AbstractTcpConnection) o;
					if (c.exception != null) {
						c.exception.call(t);
					}
				}
			} finally {
				context.remove(session.getSessionId());
			}
		}
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
			config.setHandler(new AbstractHandler() {

				@Override
				public void sessionOpened(Session session) throws Throwable {
					TcpConnectionImpl c = new TcpConnectionImpl(session);
					session.attachObject(c);
					try {
						ClientContext ctx = context.get(session.getSessionId());
						if (ctx != null && ctx.promise != null) {
							ctx.promise.succeeded(c);
						}
					} finally {
						context.remove(session.getSessionId());
					}
				}

			});
		} else {
			config.setDecoder((ByteBuffer buf, Session session) -> {
				Object o = session.getAttachment();
				if (o != null && o instanceof SecureTcpConnectionImpl) {
					SecureTcpConnectionImpl c = (SecureTcpConnectionImpl) o;
					ByteBuffer plaintext = c.sslSession.read(buf);
					if (plaintext != null && c.sslSession.isHandshakeFinished()) {
						if (c.buffer != null) {
							c.buffer.call(plaintext);
						}
					}
				}
			});
			config.setHandler(new AbstractHandler() {

				private SSLContext sslContext = config.getSslContextFactory().getSSLContext();

				@Override
				public void sessionOpened(Session session) throws Throwable {
					final SSLEngine sslEngine = sslContext.createSSLEngine();
					SSLSession sslSession = new SSLSession(sslContext, sslEngine, session, true, (ssl) -> {
						Object o = session.getAttachment();
						if (o != null && o instanceof SecureTcpConnectionImpl) {
							SecureTcpConnectionImpl c = (SecureTcpConnectionImpl) o;
							try {
								ClientContext ctx = context.get(session.getSessionId());
								if (ctx != null && ctx.promise != null) {
									ctx.promise.succeeded(c);
								}
							} finally {
								context.remove(session.getSessionId());
							}
						}
					}, new ALPN.ClientProvider() {

						@Override
						public List<String> protocols() {
							if (alpnProtocols != null) {
								return alpnProtocols.call();
							} else {
								return null;
							}
						}

						@Override
						public void unsupported() {
							if (alpnUnsupported != null) {
								alpnUnsupported.call();
							}
						}

						@Override
						public void selected(String protocol) {
							if (alpnSelected != null) {
								alpnSelected.call(protocol);
							}
						}
					});
					SecureTcpConnectionImpl c = new SecureTcpConnectionImpl(session, sslSession);
					session.attachObject(c);
				}

				@Override
				public void sessionClosed(Session session) throws Throwable {
					try {
						super.sessionClosed(session);
					} finally {
						Object o = session.getAttachment();
						if (o != null && o instanceof SecureTcpConnectionImpl) {
							SecureTcpConnectionImpl c = (SecureTcpConnectionImpl) o;
							c.sslSession.close();
						}
					}
				}
			});
		}
	}

	@Override
	protected void destroy() {
		client.stop();
	}

}
