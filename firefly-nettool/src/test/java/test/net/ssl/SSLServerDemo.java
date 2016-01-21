package test.net.ssl;

import java.security.Provider;
import java.security.Security;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.eclipse.jetty.alpn.ALPN;

import com.firefly.net.Handler;
import com.firefly.net.SSLContextFactory;
import com.firefly.net.SSLEventHandler;
import com.firefly.net.Server;
import com.firefly.net.Session;
import com.firefly.net.tcp.aio.AsynchronousTcpServer;
import com.firefly.net.tcp.ssl.DefaultCredentialSSLContextFactory;
import com.firefly.net.tcp.ssl.SSLSession;

public class SSLServerDemo {
	
	private static SSLContextFactory sslContextFactory = new DefaultCredentialSSLContextFactory();
	
	public static void main2(String[] args) {
		for(Provider provider : Security.getProviders())
			System.out.println(provider);
	}

	public static void main(String[] args) throws Throwable {
		Server server = new AsynchronousTcpServer(
		new SSLDecoder(), 
		new SSLEncoder(), 
		new Handler(){

			private SSLContext sslContext = sslContextFactory.getSSLContext();
			
			@Override
			public void sessionOpened(Session session) throws Throwable {
				long start = System.currentTimeMillis();
				final SSLEngine sslEngine = sslContext.createSSLEngine();
				System.out.println("server creates ssl engine elapsed time is " + (System.currentTimeMillis() - start));
				SessionInfo info = new SessionInfo();
				info.sslSession = new SSLSession(sslContext, sslEngine, session, false, 
				new SSLEventHandler(){

					@Override
					public void handshakeFinished(SSLSession session) {
						System.out.println("server handshake finished!");
					}}, 
				new ALPN.ServerProvider(){

					@Override
					public void unsupported() {
						System.out.println("server unsupported!");
						ALPN.remove(sslEngine);
					}

					@Override
					public String select(List<String> protocols) {
						System.out.println("client current protocols are " + protocols + "and selects " + protocols.get(0));
						ALPN.remove(sslEngine);
						return protocols.get(0);
					}});
				session.attachObject(info);
			}

			@Override
			public void sessionClosed(Session session) throws Throwable {
				System.out.println("server session closed: " + session.getSessionId());
				SessionInfo sessionAttachment = (SessionInfo)session.getAttachment();
				if(sslContext != null && sessionAttachment.sslSession != null) {
					sessionAttachment.sslSession.close();
				}
			}

			@Override
			public void messageRecieved(Session session, Object message) throws Throwable {
				System.out.println("server receives message: " + message);
				session.encode(message);
			}

			@Override
			public void exceptionCaught(Session session, Throwable t) throws Throwable {
				t.printStackTrace();
				if(session.isOpen())
					session.closeNow();
			}}, 1000 * 5);	
		server.start("localhost", 7676);
	}

}
