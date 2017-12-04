package com.firefly.net.tcp.secure.conscrypt;

import com.firefly.net.tcp.secure.utils.SecureUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * @author Pengtao Qiu
 */
public class NoCheckConscryptSSLContextFactory extends AbstractConscryptSSLContextFactory {
    @Override
    public SSLContext getSSLContext() {
        try {
            return getSSLContextWithManager(null, new TrustManager[]{SecureUtils.createX509TrustManagerNoCheck()}, null);
        } catch (Throwable e) {
            log.error("get SSL context error", e);
            return null;
        }
    }
}
