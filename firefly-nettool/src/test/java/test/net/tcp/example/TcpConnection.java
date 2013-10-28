package test.net.tcp.example;

import java.util.concurrent.BlockingQueue;

import com.firefly.net.Session;
import com.firefly.net.support.SynchronousObject;
import com.firefly.utils.collection.LinkedTransferQueue;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class TcpConnection {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private Session session;
	private long timeout;
	private BlockingQueue<MessageReceivedCallback> queue;
	
	public TcpConnection(Session session) {
		this(session, 0);
	}
	
	public TcpConnection(Session session, long timeout) {
		this.queue = new LinkedTransferQueue<MessageReceivedCallback>();
		this.session = session;
		this.timeout = timeout > 0 ? timeout : 5000L;
	}
	
	MessageReceivedCallback poll() {
		return queue.poll();
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

	public void send(Object obj, MessageReceivedCallback callback) {
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
