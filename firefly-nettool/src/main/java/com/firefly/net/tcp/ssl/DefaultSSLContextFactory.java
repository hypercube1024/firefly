package com.firefly.net.tcp.ssl;

public class DefaultSSLContextFactory extends AbstractSSLContextFactory {

    @Override
    public void createSSLContext() {
        try {
            sslContext = getSSLContextWithManager(null, null, null);
        } catch (Throwable e) {
            log.error("get SSL context error", e);
        }
    }

}
