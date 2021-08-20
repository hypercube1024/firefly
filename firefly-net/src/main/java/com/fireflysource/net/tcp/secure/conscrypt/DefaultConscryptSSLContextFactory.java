package com.fireflysource.net.tcp.secure.conscrypt;

import javax.net.ssl.SSLContext;

/**
 * @author Pengtao Qiu
 */
public class DefaultConscryptSSLContextFactory extends AbstractConscryptSecureEngineFactory {

    private SSLContext sslContext;

    public DefaultConscryptSSLContextFactory() {
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
