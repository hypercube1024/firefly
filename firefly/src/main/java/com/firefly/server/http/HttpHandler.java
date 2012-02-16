package com.firefly.server.http;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.server.exception.HttpServerException;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HttpHandler implements Handler {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private RequestHandler requestHandler;
	private HttpConnectionListener httpConnectionListener;

	public HttpHandler(HttpServletDispatcherController servletController,
			Config config) {
		httpConnectionListener = config.getHttpConnectionListener();
		String appPrefix = config.getContextPath() + config.getServletPath();
		if (VerifyUtils.isEmpty(appPrefix))
			throw new HttpServerException(
					"context path and servlet path can not be null");

		if (config.getHandlerSize() > 0) {
			requestHandler = new QueueRequestHandler(appPrefix,
					servletController, new FileDispatcherController(config),
					config.getHandlerSize());
		} else {
			requestHandler = new CurrentThreadRequestHandler(appPrefix,
					servletController, new FileDispatcherController(config));
		}
	}

	@Override
	public void sessionOpened(Session session) throws Throwable {
		httpConnectionListener.connectionCreated(session);
	}

	@Override
	public void sessionClosed(Session session) throws Throwable {
		httpConnectionListener.connectionClosed(session);
	}

	@Override
	public void messageRecieved(Session session, Object message)
			throws Throwable {
		HttpServletRequestImpl request = (HttpServletRequestImpl) message;
		requestHandler.doRequest(session, request);
	}

	@Override
	public void exceptionCaught(Session session, Throwable t) throws Throwable {
		log.error("server error", t);
		session.close(true);
	}

	public void shutdown() {
		requestHandler.shutdown();
	}
}
