package com.firefly.server.http;

import java.io.File;
import java.io.FileInputStream;

import javax.net.ssl.SSLContext;

import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLContextFactory;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HttpHandler implements Handler {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private final RequestHandler requestHandler;
	private final HttpConnectionListener httpConnectionListener;
	private SSLContext sslContext;

	public HttpHandler(HttpServletDispatcherController servletController, Config config) throws Throwable {
		httpConnectionListener = config.getHttpConnectionListener();
		requestHandler = new ThreadPoolRequestHandler(servletController);
		if(config.isSecure()) {
			if(VerifyUtils.isNotEmpty(config.getCredentialPath())
					&& VerifyUtils.isNotEmpty(config.getKeyPassword())
					&& VerifyUtils.isNotEmpty(config.getKeystorePassword())) {
				FileInputStream in = new FileInputStream(new File(config.getCredentialPath()));
				sslContext = SSLContextFactory.getSSLContext(in, config.getKeystorePassword(), config.getKeyPassword());
			} else {
				sslContext = SSLContextFactory.getSSLContext();
			}
		}
	}

	@Override
	public void sessionOpened(Session session) throws Throwable {
		SessionAttachment sessionAttachment = new SessionAttachment();
		session.attachObject(sessionAttachment);
		Monitor.CONN_COUNT.incrementAndGet();
		if(sslContext != null) {
			sessionAttachment.sslSession = new SSLSession(sslContext, session);
		}
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
		session.close(true);
	}

	public void shutdown() {
		requestHandler.shutdown();
	}

	
}
