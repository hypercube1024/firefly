package com.firefly.net.tcp.secure.conscrypt;

import com.firefly.net.tcp.secure.jdk.AbstractJdkSSLContextFactory;

import javax.net.ssl.SSLContext;

/**
 * @author Pengtao Qiu
 */
public class DefaultConscryptSSLContextFactory extends AbstractJdkSSLContextFactory {
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
