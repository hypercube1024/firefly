package test.http;

import java.io.File;

import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.server.ServerBootstrap;
import com.firefly.server.http.Config;

public class ServerDemo {

	public static void main(String[] args) throws Throwable {
		start3();
//		start2();
	}
	
	public static void start1() throws Throwable {
		SystemHtmlPage.addErrorPage(404, "/error/err404.html");
		SystemHtmlPage.addErrorPage(500, "/error/err500.html");
		String serverHome = new File(ServerBootstrap.class.getResource("/page").toURI()).getAbsolutePath();
		ServerBootstrap.start("firefly-server.xml", serverHome, "localhost", 6655);
	}
	
	public static void start2() {
		ServerBootstrap.start("firefly-server.xml");
	}
	
	public static void start3() throws Throwable {
		String serverHome = new File(ServerBootstrap.class.getResource("/page").toURI()).getAbsolutePath();
		Config config = new Config();
		config.setHost("localhost");
		config.setPort(8080);
		config.setServerHome(serverHome);
		config.setPipeline(false);
		ServerBootstrap.start(config);
	}

}
