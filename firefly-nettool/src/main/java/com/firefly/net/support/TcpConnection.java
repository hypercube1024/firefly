package com.firefly.net.support;

import java.util.concurrent.BlockingQueue;

import com.firefly.net.Session;
import com.firefly.utils.collection.LinkedTransferQueue;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class TcpConnection {
	private Session session;
	private long timeout;
	private BlockingQueue<MessageReceiveCallBack> queue;
	public static final String QUEUE_KEY = "#message_queue";
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public TcpConnection(Session session) {
		this(session, 0);
	}
	
	public TcpConnection(Session session, long timeout) {
		this.queue = new LinkedTransferQueue<MessageReceiveCallBack>();
		this.session = session;
		this.session.setAttribute(QUEUE_KEY, queue);
		this.timeout = timeout > 0 ? timeout : 5000L;
	}

	public Object send(Object obj) {
		final SynchronousObject<Object> ret = new SynchronousObject<Object>();
		send(obj, new MessageReceiveCallBack() {

			@Override
			public void messageRecieved(Session session, Object obj) {
				ret.put(obj, timeout);
			}
		});

		return ret.get(timeout);
	}

	public void send(Object obj, MessageReceiveCallBack callback) {
		if (queue.offer(callback))
			session.encode(obj);
		else
			log.warn("tcp connection queue offer failure!");
	}

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
