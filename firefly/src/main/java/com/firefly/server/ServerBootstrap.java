package com.firefly.server;

import com.firefly.mvc.web.WebContext;
import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Server;
import com.firefly.net.tcp.TcpServer;
import com.firefly.server.http.Config;
import com.firefly.server.http.HttpDecoder;
import com.firefly.server.http.HttpEncoder;
import com.firefly.server.http.HttpHandler;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ServerBootstrap {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public static void start(String serverHome, String host, int port) {
		Config config = new Config(serverHome, host, port);
		start(config);
	}

	public static void start(String configFileName, String serverHome,
			String host, int port) {
		Config config = new Config(serverHome, host, port);
		config.setConfigFileName(configFileName);
		start(config);
	}

	public static void start(Config config) {
		log.info("server home [{}]", config.getServerHome());
		log.info("context path [{}]", config.getContextPath());
		log.info("servlet path [{}]", config.getServletPath());

		long start = System.currentTimeMillis();
		WebContext context = new ServerAnnotationWebContext(config);
		HttpServletDispatcherController controller = new HttpServletDispatcherController(context);
		config.setEncoding(context.getEncoding());
		Server server = new TcpServer(new HttpDecoder(config), new HttpEncoder(), new HttpHandler(controller, config));
		server.start(config.getHost(), config.getPort());
		long end = System.currentTimeMillis();
		log.info("firefly startup in {} ms", (end - start));
	}
}
