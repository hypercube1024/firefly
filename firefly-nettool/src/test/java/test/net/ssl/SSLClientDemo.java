package test.net.ssl;

import javax.net.ssl.SSLContext;

import com.firefly.net.Client;
import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.net.tcp.aio.AsynchronousTcpClient;
import com.firefly.net.tcp.ssl.SSLContextFactory;
import com.firefly.net.tcp.ssl.SSLEventHandler;
import com.firefly.net.tcp.ssl.SSLSession;

public class SSLClientDemo {

	public static void main(String[] args) throws Throwable {
		Client client = new AsynchronousTcpClient(
		new SSLDecoder(), 
		new SSLEncoder(),
		new Handler(){
			private SSLContext sslContext = SSLContextFactory.getSSLContext();
			
			@Override
			public void sessionOpened(final Session session) throws Throwable {
			
				SessionInfo info = new SessionInfo();
				info.sslSession = new SSLSession(sslContext, session, true, new SSLEventHandler(){

					@Override
					public void handshakeFinished(SSLSession sslSession) {
						session.encode("hello world SSL client!");
					}});
				session.attachObject(info);
			}

			@Override
			public void sessionClosed(Session session) throws Throwable {
				System.out.println("client session close " + session.getSessionId());
			}

			@Override
			public void messageRecieved(Session session, Object message) throws Throwable {
				System.out.println("client receive message: " + message);
				
			}

			@Override
			public void exceptionCaught(Session session, Throwable t)
					throws Throwable {
				t.printStackTrace();
			}}, 1000 * 1000 * 60);
		
		int sessionId = client.connect("127.0.0.1", 7676);
		System.out.println("client session id: " + sessionId);
	}

}
