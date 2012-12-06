package com.firefly.benchmark;

import java.io.File;

import com.firefly.server.ServerBootstrap;
import com.firefly.server.http.Config;

public class Bootstrap {

	public static void main(String[] args) throws Throwable {
		String projectHome = new File(Bootstrap.class.getResource("/").toURI()).getParent();
		String serverHome = new File(projectHome, "/page").getAbsolutePath();
		Config config = new Config();
		config.setHost("localhost");
		config.setPort(6655);
		config.setServerHome(serverHome);
		config.setPipeline(true);
		ServerBootstrap.start(config);
	}

}
