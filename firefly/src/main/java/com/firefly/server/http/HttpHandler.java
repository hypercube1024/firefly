package com.firefly.server.http;

import javax.net.ssl.SSLContext;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Handler;
import com.firefly.net.SSLEventHandler;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.server.Config;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HttpHandler implements Handler {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private final RequestHandler requestHandler;
	private final HttpConnectionListener httpConnectionListener;
	private SSLContext sslContext;

	public HttpHandler(HttpServletDispatcherController servletController, Config config) throws Throwable {
		httpConnectionListener = config.getHttpConnectionListener();
		
		log.info("request handler [{}]", config.getRequestHandler());
		ThreadPoolWrapper.init(config);
		switch (config.getRequestHandler()) {
		case "threadPool":
			requestHandler = new ThreadPoolRequestHandler(servletController);
			break;
		case "currentThread":
			requestHandler = new CurrentThreadRequestHandler(servletController);
			break;
		default:
			requestHandler = new CurrentThreadRequestHandler(servletController);
			break;
		}
		
		if(config.isSecure()) {
			sslContext = config.getSslContextFactory().getSSLContext();
		}
	}

	@Override
	public void sessionOpened(Session session) throws Throwable {
		SessionAttachment sessionAttachment = new SessionAttachment();
		session.attachObject(sessionAttachment);
		Monitor.CONN_COUNT.incrementAndGet();
		if(sslContext != null) {
			sessionAttachment.sslSession = new SSLSession(sslContext, session, false, new SSLEventHandler(){

				@Override
				public void handshakeFinished(SSLSession session) {
					
					
				}});
		}
		httpConnectionListener.connectionCreated(session);
	}

	@Override
	public void sessionClosed(Session session) throws Throwable {
		Monitor.CONN_COUNT.decrementAndGet();
		
		httpConnectionListener.connectionClosed(session);
		SessionAttachment sessionAttachment = (SessionAttachment)session.getAttachment();
		if(sslContext != null && sessionAttachment.sslSession != null) {
			sessionAttachment.sslSession.close();
		}
	}

	@Override
	public void messageRecieved(Session session, Object message) throws Throwable {
		HttpServletRequestImpl request = (HttpServletRequestImpl) message;
		requestHandler.doRequest(session, request);
	}

	@Override
	public void exceptionCaught(Session session, Throwable t) throws Throwable {
		log.error("server handle error", t);
		session.closeNow();
	}

	public void shutdown() {
		requestHandler.shutdown();
	}

	
}
