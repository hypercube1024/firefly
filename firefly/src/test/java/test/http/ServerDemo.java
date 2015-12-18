package test.http;

import com.firefly.server.ServerBootstrap;

public class ServerDemo {

	public static void main(String[] args) throws Throwable {
		start1();
	}
	
	public static void start1() {
		ServerBootstrap.start("firefly-server.xml");
	}
	

}
