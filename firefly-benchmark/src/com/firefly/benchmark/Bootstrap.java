package com.firefly.benchmark;

import java.io.File;

import com.firefly.mvc.web.servlet.SystemHtmlPage;
import com.firefly.server.ServerBootstrap;
import com.firefly.server.http.Config;

public class Bootstrap {

	public static void main(String[] args) throws Throwable {
		SystemHtmlPage.addErrorPage(404, "/error/e404.html");
		
		String projectHome = new File(Bootstrap.class.getResource("/").toURI()).getParent();
		String serverHome = new File(projectHome, "/page").getAbsolutePath();
		Config config = new Config();
		config.setHost("localhost");
		config.setPort(8080);
		config.setServerHome(serverHome);
		ServerBootstrap.start(config);
	}

}
