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

	public static void start() {
		ServerBootstrap bootstrap = new ServerBootstrap("firefly-server2.xml");
		bootstrap.start();
	}

}
