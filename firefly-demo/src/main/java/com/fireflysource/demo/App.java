package com.fireflysource.demo;

import java.io.File;

import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.server.Config;
import com.firefly.server.ServerBootstrap;

/**
 * Hello world!
 *
 */
public class App {
	
	public static void main(String[] args) throws Throwable {
		SystemHtmlPage.addErrorPage(404, "/error/e404.html");
		
		String projectHome = new File(App.class.getResource("/").toURI()).getParentFile().getParent();
		String serverHome = new File(projectHome, "/page").getAbsolutePath();
		
		System.out.println(projectHome);
		System.out.println(serverHome);
		Config config = new Config();
		config.setHost("localhost");
		config.setPort(8080);
		config.setServerHome(serverHome);
//		config.setSecure(true);
		ServerBootstrap.start(config);
	}
}
