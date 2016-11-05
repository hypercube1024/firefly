package com.firefly.server.http2.servlet;

import com.firefly.mvc.web.WebContext;
import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.server.http2.HTTP2Server;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.lang.AbstractLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;

public class ServerBootstrap extends AbstractLifeCycle {

	private static Logger log = LoggerFactory.getLogger("firefly-system");

	private final HTTP2Server http2Server;
	private final long createTime = System.currentTimeMillis();

	public ServerBootstrap(ServerHTTP2Configuration http2Configuration) {
		WebContext context = new ServerAnnotationWebContext(http2Configuration);
		this.http2Server = new HTTP2Server(http2Configuration.getHost(), http2Configuration.getPort(),
				http2Configuration,
				new ServletServerHTTPHandler(http2Configuration, new HttpServletDispatcherController(context)));
	}

	public ServerBootstrap(String configFileName) {
		if (VerifyUtils.isEmpty(configFileName)) {
			configFileName = ServerHTTP2Configuration.DEFAULT_CONFIG_FILE_NAME;
		}

		WebContext context = new ServerAnnotationWebContext(configFileName);
		ServerHTTP2Configuration http2Configuration = context.getBean(ServerHTTP2Configuration.class);
		this.http2Server = new HTTP2Server(http2Configuration.getHost(), http2Configuration.getPort(),
				http2Configuration,
				new ServletServerHTTPHandler(http2Configuration, new HttpServletDispatcherController(context)));
	}

	public ServerBootstrap(String host, int port) {
		this(null, host, port);
	}

	public ServerBootstrap(String configFileName, String host, int port) {
		if (VerifyUtils.isEmpty(configFileName)) {
			configFileName = ServerHTTP2Configuration.DEFAULT_CONFIG_FILE_NAME;
		}

		WebContext context = new ServerAnnotationWebContext(configFileName);
		ServerHTTP2Configuration http2Configuration = context.getBean(ServerHTTP2Configuration.class);
		if (http2Configuration == null) {
			http2Configuration = new ServerHTTP2Configuration();
		}

		http2Configuration.setHost(host);
		http2Configuration.setPort(port);
		this.http2Server = new HTTP2Server(http2Configuration.getHost(), http2Configuration.getPort(),
				http2Configuration,
				new ServletServerHTTPHandler(http2Configuration, new HttpServletDispatcherController(context)));
	}

	@Override
	protected void init() {
		File file = new File(((ServerHTTP2Configuration) http2Server.getHttp2Configuration()).getTemporaryDirectory());
		if (!file.exists()) {
			file.mkdirs();
		}

		http2Server.start();

		String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		log.info("the jvm name is {}", jvmName);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("the process " + jvmName + " will stop");
			this.stop();
		}));

		log.info("the server start spends time in {} ms", System.currentTimeMillis() - createTime);
	}

	@Override
	protected void destroy() {
		AsyncContextImpl.shutdown();
		http2Server.stop();
		((ServerHTTP2Configuration) http2Server.getHttp2Configuration()).getHttpSessionManager().stop();
	}

}
