package com.fireflysource.demo;

import java.io.File;

import com.firefly.server.ServerBootstrap;
import com.firefly.server.http.Config;

/**
 * Hello world!
 *
 */
public class App {
	
	public static void main(String[] args) throws Throwable {
		String projectHome = new File(App.class.getResource("/").toURI()).getParentFile().getParent();
		String serverHome = new File(projectHome, "/page").getAbsolutePath();
		
		System.out.println(projectHome);
		System.out.println(serverHome);
		Config config = new Config();
		config.setHost("localhost");
		config.setPort(8080);
		config.setServerHome(serverHome);
		ServerBootstrap.start(config);
	}
}
