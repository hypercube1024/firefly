package com.fireflysource.net.tcp.secure.conscrypt;

import javax.net.ssl.SSLContext;

/**
 * @author Pengtao Qiu
 */
public class DefaultConscryptSSLContextFactory extends AbstractConscryptSecureEngineFactory {
    @Override
    public SSLContext getSSLContext() {
        try {
            return getSSLContextWithManager(null, null, null);
        } catch (Throwable e) {
            log.error(e, () -> "get SSL context error");
            return null;
        }
    }
}
