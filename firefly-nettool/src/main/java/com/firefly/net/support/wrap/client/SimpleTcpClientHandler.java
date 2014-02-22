package com.firefly.net.support.wrap.client;

import java.util.Map;

import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

abstract public class SimpleTcpClientHandler implements Handler {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private volatile Map<Integer, ConnectionInfo> connectionInfo;
	
	public void setConnectionInfo(Map<Integer, ConnectionInfo> connectionInfo) {
		if(this.connectionInfo != null)
			return;	
		
		this.connectionInfo = connectionInfo;
	}

    @Override
    public final void sessionOpened(Session session) throws Throwable {
        ConnectionInfo info = connectionInfo.get(session.getSessionId());
        if(info == null) 
        	return;
        
        try {
	        SessionAttachment sessionInfo = new SessionAttachment();
	        TcpConnection connection = createTcpConnection(session, info.timeout);
	        sessionInfo.connection = connection;
	        session.attachObject(sessionInfo);
	        info.callback.sessionOpened(connection);
        } finally {
        	connectionInfo.remove(session.getSessionId());
        }
    }
    
    abstract public TcpConnection createTcpConnection(Session session, long timeout);
    
    @Override
    public void sessionClosed(Session session) throws Throwable {
        log.debug("session: {} close", session.getSessionId());
    }

	@Override
    public final void messageRecieved(Session session, Object message) throws Throwable {
		SessionAttachment sessionInfo = (SessionAttachment)session.getAttachment();
		messageRecieved(sessionInfo.connection, message);
    }
	
	abstract public void messageRecieved(TcpConnection connection, Object message) throws Throwable ;

    @Override
    public void exceptionCaught(Session session, Throwable t) throws Throwable {
        log.error("client session error", t);
        session.close(true);
    }

}
