package com.firefly.benchmark;

import java.io.File;

import com.firefly.server.ServerBootstrap;

public class Bootstrap {

	public static void main(String[] args) throws Throwable {
		String serverHome = new File(new File(Bootstrap.class.getResource("/")
				.toURI()).getParent(), "/page").getAbsolutePath();
		ServerBootstrap.start(serverHome, "localhost", 6655);
	}

}
