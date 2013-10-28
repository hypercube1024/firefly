package test.net.tcp.example;

import java.util.Map;

import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class SimpleTcpClientHandler implements Handler {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private static final String CONNECTION_KEY = "#CONNECTION_KEY";
	private Map<Integer, ConnectionInfo> connectionInfo;
	
	public SimpleTcpClientHandler(Map<Integer, ConnectionInfo> connectionInfo) {
		this.connectionInfo = connectionInfo;
	}

    @Override
    public void sessionOpened(Session session) throws Throwable {
        ConnectionInfo info = connectionInfo.get(session.getSessionId());
        if(info == null) 
        	return;
        
        TcpConnection connection = new TcpConnection(session, info.timeout);
        session.setAttribute(CONNECTION_KEY, connection);
        info.callback.sessionOpened(connection);
        connectionInfo.remove(session.getSessionId());
    }

    @Override
    public void sessionClosed(Session session) throws Throwable {
        log.info("session: {} close", session.getSessionId());
    }

	@Override
    public void messageRecieved(Session session, Object message) throws Throwable {
        TcpConnection conn = (TcpConnection)session.getAttribute(CONNECTION_KEY);
        log.debug("message received : {}", conn.getId());
        conn.poll().messageRecieved(conn, message);
    }

    @Override
    public void exceptionCaught(Session session, Throwable t) throws Throwable {
        log.error("client session error", t);
        session.close(true);
    }

}
