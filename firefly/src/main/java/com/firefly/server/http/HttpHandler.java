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
	
	public static final String SSL_SESSION_KEY = "_sslSession#";

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
		Monitor.CONN_COUNT.incrementAndGet();
		
		if(sslContext != null)
			session.setAttribute(SSL_SESSION_KEY, new SSLSession(sslContext, session));
		
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
		
		if(sslContext != null) {
			SSLSession sslSession = (SSLSession) session.getAttribute(SSL_SESSION_KEY);
			if(sslSession != null) {
				sslSession.close();
			}
		}
		session.clearAttributes();
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
