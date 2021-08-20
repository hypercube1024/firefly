package com.fireflysource.net.tcp.secure.wildfly;

import javax.net.ssl.SSLContext;

public class DefaultWildflySSLContextFactory extends AbstractWildflySecureEngineFactory {
    private SSLContext sslContext;

    public DefaultWildflySSLContextFactory() {
        try {
            sslContext = getSSLContextWithManager(null, null, null);
        } catch (Throwable e) {
            LOG.error("get SSL context error", e);
        }
    }

    @Override
    public SSLContext getSSLContext() {
        return sslContext;
    }
}
