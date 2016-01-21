package com.firefly.net.tcp.ssl;

import javax.net.ssl.SSLContext;

public class DefaultSSLContextFactory extends AbstractSSLContextFactory {

	@Override
	public SSLContext getSSLContext() {
		try {
			return getSSLContextWithManager(null, null, null);
		} catch (Throwable e) {
			log.error("get SSL context error", e);
			return null;
		}
	}

}
