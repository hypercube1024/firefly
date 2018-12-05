package com.fireflysource.net.tcp.secure.conscrypt;


import com.fireflysource.net.tcp.secure.utils.SecureUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * @author Pengtao Qiu
 */
public class NoCheckConscryptSSLContextFactory extends AbstractConscryptSecureEngineFactory {
    @Override
    public SSLContext getSSLContext() {
        try {
            return getSSLContextWithManager(null, new TrustManager[]{SecureUtils.createX509TrustManagerNoCheck()}, null);
        } catch (Throwable e) {
            log.error(e, () -> "get SSL context error");
            return null;
        }
    }
}
