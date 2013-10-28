package test.net.tcp.example;

import java.util.Map;

import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SimpleTcpClientHandler implements Handler {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private Map<Integer, ConnectionInfo> connectionInfo;
	
	public SimpleTcpClientHandler(Map<Integer, ConnectionInfo> connectionInfo) {
		this.connectionInfo = connectionInfo;
	}

    @Override
    public void sessionOpened(Session session) throws Throwable {
        ConnectionInfo info = connectionInfo.get(session.getSessionId());
        if(info == null) 
        	return;
        
        SessionInfo sessionInfo = new SessionInfo();
        TcpConnection connection = new TcpConnection(session, info.timeout);
        sessionInfo.connection = connection;
        session.attachObject(sessionInfo);
        info.callback.sessionOpened(connection);
        connectionInfo.remove(session.getSessionId());
    }

    @Override
    public void sessionClosed(Session session) throws Throwable {
        log.info("session: {} close", session.getSessionId());
    }

	@Override
    public void messageRecieved(Session session, Object message) throws Throwable {
		SessionInfo sessionInfo = (SessionInfo)session.getAttachment();
		sessionInfo.connection.poll().messageRecieved(sessionInfo.connection, message);
    }

    @Override
    public void exceptionCaught(Session session, Throwable t) throws Throwable {
        log.error("client session error", t);
        session.close(true);
    }

}
