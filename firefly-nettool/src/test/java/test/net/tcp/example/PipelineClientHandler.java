package test.net.tcp.example;

import com.firefly.net.Session;
import com.firefly.net.support.wrap.client.SimpleTcpClientHandler;
import com.firefly.net.support.wrap.client.TcpConnection;

public class PipelineClientHandler extends SimpleTcpClientHandler {

	@Override
	public TcpConnection createTcpConnection(Session session, long timeout) {
		return new PipelineTcpConnection(session, timeout);
	}

	@Override
	public void messageRecieved(TcpConnection connection, Object message) throws Throwable {
		PipelineTcpConnection conn = (PipelineTcpConnection) connection;
		conn.poll().messageRecieved(connection, message);
	}

}
