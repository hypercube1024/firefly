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
            LOG.error(e, () -> "get SSL context error");
        }
    }

    @Override
    public SSLContext getSSLContext() {
        return sslContext;
    }
}
