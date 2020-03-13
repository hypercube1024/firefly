package com.fireflysource.net.tcp.secure.conscrypt;


import com.fireflysource.net.tcp.secure.utils.SecureUtils;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

/**
 * @author Pengtao Qiu
 */
public class SelfSignedCredentialConscryptSSLContextFactory extends AbstractConscryptSecureEngineFactory {

    private SSLContext sslContext;

    public SelfSignedCredentialConscryptSSLContextFactory() {
        try (InputStream in = SecureUtils.getSelfSignedCredential()) {
            sslContext = getSSLContext(in, "123456", null);
        } catch (Throwable e) {
            LOG.error(e, () -> "get SSL context error");
        }
    }

    @Override
    public SSLContext getSSLContext() {
        return sslContext;
    }
}
