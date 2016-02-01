package com.firefly.server.http2.servlet;

import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.core.AbstractLifeCycle;
import com.firefly.mvc.web.WebContext;
import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.server.http2.HTTP2Server;

public class ServerBootstrap extends AbstractLifeCycle {

	private final HTTP2Server http2Server;

	public ServerBootstrap(HTTP2Configuration http2Configuration) {
		WebContext context = new ServerAnnotationWebContext(http2Configuration);
		HttpServletDispatcherController controller = new HttpServletDispatcherController(context);
		this.http2Server = new HTTP2Server(http2Configuration.getHost(), http2Configuration.getPort(),
				http2Configuration, new ServletServerHTTPHandler(http2Configuration, controller));
	}

	public ServerBootstrap(String serverHome, String host, int port) {
		HTTP2Configuration http2Configuration = new HTTP2Configuration();
		http2Configuration.setServerHome(serverHome);
		http2Configuration.setHost(host);
		http2Configuration.setPort(port);
		WebContext context = new ServerAnnotationWebContext(http2Configuration);
		HttpServletDispatcherController controller = new HttpServletDispatcherController(context);
		this.http2Server = new HTTP2Server(http2Configuration.getHost(), http2Configuration.getPort(),
				http2Configuration, new ServletServerHTTPHandler(http2Configuration, controller));
	}

	public ServerBootstrap(String configFileName) {
		WebContext context = new ServerAnnotationWebContext(configFileName);
		HTTP2Configuration http2Configuration = context.getBean(HTTP2Configuration.class);
		HttpServletDispatcherController controller = new HttpServletDispatcherController(context);
		this.http2Server = new HTTP2Server(http2Configuration.getHost(), http2Configuration.getPort(),
				http2Configuration, new ServletServerHTTPHandler(http2Configuration, controller));
	}

	@Override
	protected void init() {
		http2Server.start();
	}

	@Override
	protected void destroy() {
		AsyncContextImpl.shutdown();
		http2Server.stop();
	}

}
