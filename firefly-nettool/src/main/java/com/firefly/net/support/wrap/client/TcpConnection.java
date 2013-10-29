package com.firefly.net.support.wrap.client;

import com.firefly.net.Session;
import com.firefly.net.support.SynchronousObject;
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

	public Object send(Object obj) {
		final SynchronousObject<Object> ret = new SynchronousObject<Object>();
		send(obj, new MessageReceivedCallback() {

			@Override
			public void messageRecieved(TcpConnection connection, Object obj) {
				ret.put(obj, timeout);
			}
		});

		return ret.get(timeout);
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
