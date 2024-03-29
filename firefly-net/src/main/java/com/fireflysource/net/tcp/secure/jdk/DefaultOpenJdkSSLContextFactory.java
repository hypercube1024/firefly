package com.fireflysource.net.tcp.secure.jdk;

import javax.net.ssl.SSLContext;

/**
 * @author Pengtao Qiu
 */
public class DefaultOpenJdkSSLContextFactory extends AbstractOpenJdkSecureEngineFactory {

    private SSLContext sslContext;

    public DefaultOpenJdkSSLContextFactory() {
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
