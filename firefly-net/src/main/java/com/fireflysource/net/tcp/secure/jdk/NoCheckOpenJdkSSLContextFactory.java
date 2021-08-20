package com.fireflysource.net.tcp.secure.jdk;


import com.fireflysource.net.tcp.secure.utils.SecureUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * @author Pengtao Qiu
 */
public class NoCheckOpenJdkSSLContextFactory extends AbstractOpenJdkSecureEngineFactory {

    private SSLContext sslContext;

    public NoCheckOpenJdkSSLContextFactory() {
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
