package test.net.tcp.example;

import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;

import com.firefly.net.Session;
import com.firefly.net.support.wrap.client.MessageReceivedCallback;
import com.firefly.net.support.wrap.client.TcpConnection;

public class PipelineTcpConnection extends TcpConnection {
	
	private Queue<MessageReceivedCallback> queue;

	public PipelineTcpConnection(Session session, long timeout) {
		super(session, timeout);
		this.queue = new LinkedTransferQueue<MessageReceivedCallback>();
	}
	
	MessageReceivedCallback poll() {
		return queue.poll();
	}

	@Override
	public void send(Object obj, MessageReceivedCallback callback) {
		if (queue.offer(callback))
			session.encode(obj);
		else
			log.warn("tcp connection queue offer failure!");
	}

}
