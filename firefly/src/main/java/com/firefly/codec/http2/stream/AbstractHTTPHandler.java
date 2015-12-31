package com.firefly.codec.http2.stream;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;

import com.firefly.net.Handler;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLContextFactory;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class AbstractHTTPHandler implements Handler {

	protected static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	protected final HTTP2Configuration config;
	protected final List<String> protocols = Arrays.asList("h2", "h2-17", "h2-16", "h2-15", "h2-14", "http/1.1");
	protected SSLContext sslContext;

	public AbstractHTTPHandler(HTTP2Configuration config) {
		this.config = config;
		try {
			if (config.isSecure()) {
				if (VerifyUtils.isNotEmpty(config.getCredentialPath())
						&& VerifyUtils.isNotEmpty(config.getKeyPassword())
						&& VerifyUtils.isNotEmpty(config.getKeystorePassword())) {
					FileInputStream in = new FileInputStream(new File(config.getCredentialPath()));
					sslContext = SSLContextFactory.getSSLContext(in, config.getKeystorePassword(),
							config.getKeyPassword());
				} else {
					sslContext = SSLContextFactory.getSSLContext();
				}
			}
		} catch (Throwable t) {
			log.error("create ssl context error", t);
		}
	}

	@Override
	public void messageRecieved(Session session, Object message) throws Throwable {
	}

	@Override
	public void exceptionCaught(Session session, Throwable t) throws Throwable {
		log.error("HTTP handler exception", t);
		HTTPConnection httpConnection = (HTTPConnection) session.getAttachment();
		if (httpConnection != null && httpConnection.isOpen()) {
			httpConnection.close();
		}
	}
	
	@Override
	public void sessionClosed(Session session) throws Throwable {
		log.info("session {} closed", session.getSessionId());
		try {
			if(session.getAttachment() instanceof HTTPConnection) {
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
