package test.http;

import java.io.File;

import com.firefly.server.ServerBootstrap;

public class ServerDemo {

	public static void main(String[] args) throws Throwable {
//		start1();
		start2();
	}
	
	public static void start1() throws Throwable {
		String serverHome = new File(ServerBootstrap.class.getResource("/page").toURI()).getAbsolutePath();
		ServerBootstrap.start("firefly-server.xml", serverHome, "localhost", 6655);
	}
	
	public static void start2() {
		ServerBootstrap.start("firefly-server.xml");
	}

}
