package com.firefly.net.tcp.secure;

import javax.net.ssl.SSLContext;

/**
 * @author Pengtao Qiu
 */
public class DefaultJdkSSLContextFactory extends AbstractJdkSSLContextFactory {
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
