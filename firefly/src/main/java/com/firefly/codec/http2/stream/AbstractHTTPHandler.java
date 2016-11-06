package com.firefly.codec.http2.stream;

import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.server.utils.StatisticsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractHTTPHandler implements Handler {

	protected static Logger log = LoggerFactory.getLogger("firefly-system");

	protected final HTTP2Configuration config;
	protected final List<String> protocols = Arrays.asList("h2", "h2-17", "h2-16", "h2-15", "h2-14", "http/1.1");
	protected SSLContext sslContext;

	public AbstractHTTPHandler(HTTP2Configuration config) {
		this.config = config;
		if (config.isSecureConnectionEnabled()) {
			sslContext = config.getSslContextFactory().getSSLContext();
		}
	}

	@Override
	public void messageRecieved(Session session, Object message) throws Throwable {
	}

	@Override
	public void exceptionCaught(Session session, Throwable t) throws Throwable {
		log.error("HTTP handler exception", t);
		if (session.getAttachment() instanceof HTTPConnection) {
			HTTPConnection httpConnection = (HTTPConnection) session.getAttachment();
			if (httpConnection != null && httpConnection.isOpen()) {
				httpConnection.close();
			}
		}
	}

	@Override
	public void sessionClosed(Session session) throws Throwable {
		log.info("session {} closed", session.getSessionId());
		StatisticsUtils.saveConnectionInfo(session);
		try {
			if (session.getAttachment() instanceof HTTPConnection) {
				HTTPConnection httpConnection = (HTTPConnection) session.getAttachment();
				if (httpConnection != null && httpConnection.isOpen()) {
					httpConnection.close();
				}
			}
		} catch (Throwable t) {
			log.error("http2 conection close exception", t);
		}
	}

}
