package test.net.ssl;

import javax.net.ssl.SSLContext;

import com.firefly.net.Handler;
import com.firefly.net.Server;
import com.firefly.net.Session;
import com.firefly.net.tcp.aio.AsynchronousTcpServer;
import com.firefly.net.tcp.ssl.SSLContextFactory;
import com.firefly.net.tcp.ssl.SSLEventHandler;
import com.firefly.net.tcp.ssl.SSLSession;

public class SSLServerDemo {

	public static void main(String[] args) throws Throwable {
		Server server = new AsynchronousTcpServer(
		new SSLDecoder(), 
		new SSLEncoder(), 
		new Handler(){

			private SSLContext sslContext = SSLContextFactory.getSSLContext();
			
			@Override
			public void sessionOpened(Session session) throws Throwable {
				SessionInfo info = new SessionInfo();
				info.sslSession = new SSLSession(sslContext, session, false, new SSLEventHandler(){

					@Override
					public void handshakeFinished(SSLSession session) {
						
						
					}});
				session.attachObject(info);
			}

			@Override
			public void sessionClosed(Session session) throws Throwable {
				System.out.println("server session close: " + session.getSessionId());
				SessionInfo sessionAttachment = (SessionInfo)session.getAttachment();
				if(sslContext != null && sessionAttachment.sslSession != null) {
					sessionAttachment.sslSession.close();
				}
			}

			@Override
			public void messageRecieved(Session session, Object message) throws Throwable {
				System.out.println("server receive message: " + message);
				session.encode(message);
			}

			@Override
			public void exceptionCaught(Session session, Throwable t) throws Throwable {
				t.printStackTrace();
				session.close(true);
			}}, 1000 * 5);	
		server.start("localhost", 7676);
	}

}
