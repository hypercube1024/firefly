package com.firefly.net.support;

import java.util.Queue;

import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SimpleTcpClientHandler implements Handler {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private Synchronizer<Session> synchronizer;
	
	public SimpleTcpClientHandler(Synchronizer<Session> synchronizer) {
		this.synchronizer = synchronizer;
	}

    @Override
    public void sessionOpened(Session session) throws Throwable {
        log.debug("session: {} open", session.getSessionId());
        synchronizer.put(session.getSessionId(), session);
    }

    @Override
    public void sessionClosed(Session session) throws Throwable {
        log.debug("session: {} close", session.getSessionId());
    }

    @SuppressWarnings("unchecked")
	@Override
    public void messageRecieved(Session session, Object message) throws Throwable {
        log.debug("message: {}", message);
        Queue<MessageReceiveCallBack> queue = (Queue<MessageReceiveCallBack>)session.getAttribute(TcpConnection.QUEUE_KEY);
        queue.poll().messageRecieved(session, message);
    }

    @Override
    public void exceptionCaught(Session session, Throwable t) throws Throwable {
        log.error("client session error", t);
        session.close(true);
    }

}
