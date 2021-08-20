package com.fireflysource.net.tcp.secure.wildfly;

import com.fireflysource.net.tcp.secure.utils.SecureUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public class NoCheckWildflySSLContextFactory extends AbstractWildflySecureEngineFactory{

    private SSLContext sslContext;

    public NoCheckWildflySSLContextFactory() {
        try {
            sslContext = getSSLContextWithManager(null, new TrustManager[]{SecureUtils.createX509TrustManagerNoCheck()}, null);
        } catch (Throwable e) {
            LOG.error("get SSL context error", e);
        }
    }

    @Override
    public SSLContext getSSLContext() {
        return sslContext;
    }
}
