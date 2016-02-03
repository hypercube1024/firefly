package com.firefly.server.http2.servlet;

import java.io.File;

import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.core.AbstractLifeCycle;
import com.firefly.mvc.web.WebContext;
import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ServerBootstrap extends AbstractLifeCycle {

	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	private final HTTP2Server http2Server;
	private final long createTime = System.currentTimeMillis();

	public ServerBootstrap(HTTP2Configuration http2Configuration) {
		WebContext context = new ServerAnnotationWebContext(http2Configuration);
		this.http2Server = new HTTP2Server(http2Configuration.getHost(), http2Configuration.getPort(),
				http2Configuration,
				new ServletServerHTTPHandler(http2Configuration, new HttpServletDispatcherController(context)));
	}

	public ServerBootstrap(String configFileName) {
		if (VerifyUtils.isEmpty(configFileName)) {
			configFileName = HTTP2Configuration.DEFAULT_CONFIG_FILE_NAME;
		}

		WebContext context = new ServerAnnotationWebContext(configFileName);
		HTTP2Configuration http2Configuration = context.getBean(HTTP2Configuration.class);
		this.http2Server = new HTTP2Server(http2Configuration.getHost(), http2Configuration.getPort(),
				http2Configuration,
				new ServletServerHTTPHandler(http2Configuration, new HttpServletDispatcherController(context)));
	}

	public ServerBootstrap(String host, int port) {
		this(null, host, port);
	}

	public ServerBootstrap(String configFileName, String host, int port) {
		if (VerifyUtils.isEmpty(configFileName)) {
			configFileName = HTTP2Configuration.DEFAULT_CONFIG_FILE_NAME;
		}

		WebContext context = new ServerAnnotationWebContext(configFileName);
		HTTP2Configuration http2Configuration = context.getBean(HTTP2Configuration.class);
		if (http2Configuration == null) {
			http2Configuration = new HTTP2Configuration();
		}

		http2Configuration.setHost(host);
		http2Configuration.setPort(port);
		this.http2Server = new HTTP2Server(http2Configuration.getHost(), http2Configuration.getPort(),
				http2Configuration,
				new ServletServerHTTPHandler(http2Configuration, new HttpServletDispatcherController(context)));
	}

	@Override
	protected void init() {
		File file = new File(http2Server.getHttp2Configuration().getTemporaryDirectory());
		if(!file.exists()) {
			file.mkdirs();
		}
		
		http2Server.start();
		log.info("the server start spends time in {} ms", System.currentTimeMillis() - createTime);
	}

	@Override
	protected void destroy() {
		AsyncContextImpl.shutdown();
		http2Server.stop();
	}

}
