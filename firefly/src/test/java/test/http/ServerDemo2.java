package test.http;

import com.firefly.server.http2.servlet.ServerBootstrap;

public class ServerDemo2 {

	/**
	 * Enable ALPN support -Xbootclasspath/p:[path_to_alpn_boot_jar]
	 * @param args
	 * @throws Throwable
	 */
	public static void main(String[] args) throws Throwable {
//		System.setProperty("javax.net.debug", "all");
		start();
	}

	/**
	 * Generate SSL credentials:
	 * keytool -genkeypair -alias fireflySSLkeys -keyalg RSA -keystore fireflySSLkeys.jks
	 * 
	 * List credentials in key store
	 * keytool -list -keystore fireflySSLkeys.jks
	 * @throws InterruptedException 
	 */
	public static void start() throws InterruptedException {
		ServerBootstrap bootstrap = new ServerBootstrap("firefly-server2.xml");
		bootstrap.start();
		
//		Thread.sleep(2000L);
//		bootstrap.stop();
	}

}
