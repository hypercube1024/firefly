package com.firefly.server.http;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HttpHandler implements Handler {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private final RequestHandler requestHandler;
	private final HttpConnectionListener httpConnectionListener;

	public HttpHandler(HttpServletDispatcherController servletController, Config config) {
		httpConnectionListener = config.getHttpConnectionListener();
		requestHandler = new QueueRequestHandler(servletController);
	}

	@Override
	public void sessionOpened(Session session) throws Throwable {
		Monitor.CONN_COUNT.incrementAndGet();
		httpConnectionListener.connectionCreated(session);
	}

	@Override
	public void sessionClosed(Session session) throws Throwable {
		log.info("connection close|{}|{}|{}|{}", 
				session.getSessionId(), 
				session.getReadBytes(), 
				session.getWrittenBytes(),
				Monitor.CONN_COUNT.decrementAndGet());
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
