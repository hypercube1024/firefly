package test.http;

import com.firefly.server.http2.servlet.ServerBootstrap;

public class ServerDemo1 {

	public static void main(String[] args) throws Throwable {
		ServerBootstrap bootstrap = new ServerBootstrap("firefly-server1.xml", "localhost", 6656);
		bootstrap.start();
		Thread.sleep(2000L);
		bootstrap.stop();
	}

}
