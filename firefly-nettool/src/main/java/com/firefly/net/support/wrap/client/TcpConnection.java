package com.firefly.net.support.wrap.client;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

abstract public class TcpConnection {
	protected static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	protected Session session;
	protected long timeout;
	
	public TcpConnection(Session session) {
		this(session, 0);
	}
	
	public TcpConnection(Session session, long timeout) {
		this.session = session;
		this.timeout = timeout > 0 ? timeout : 5000L;
	}

	public Future<Object> send(Object obj) {
		final ResultCallable<Object> callable = new ResultCallable<Object>();
		final FutureTask<Object> future = new FutureTask<Object>(callable);
		
		send(obj, new MessageReceivedCallback() {

			@Override
			public void messageRecieved(TcpConnection connection, Object resultObject) {
				callable.setValue(resultObject);
				future.run();
			}
		});

		return future;
	}

	abstract public void send(Object obj, MessageReceivedCallback callback);


	public int getId() {
		return session.getSessionId();
	}

	public void close(boolean b) {
		session.close(b);
	}

	public boolean isOpen() {
		return session.isOpen();
	}

	public Session getSession() {
		return session;
	}
}
