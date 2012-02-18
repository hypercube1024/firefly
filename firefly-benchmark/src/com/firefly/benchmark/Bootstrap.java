package com.firefly.benchmark;

import java.io.File;

import com.firefly.server.ServerBootstrap;

public class Bootstrap {

	public static void main(String[] args) throws Throwable {
		String projectHome = new File(Bootstrap.class.getResource("/").toURI()).getParent();
		String serverHome = new File(projectHome, "/page").getAbsolutePath();
		ServerBootstrap.start(serverHome, "localhost", 6655);
	}

}
