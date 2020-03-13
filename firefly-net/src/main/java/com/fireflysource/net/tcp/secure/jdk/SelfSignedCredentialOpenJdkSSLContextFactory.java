package com.fireflysource.net.tcp.secure.jdk;


import com.fireflysource.net.tcp.secure.utils.SecureUtils;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

/**
 * @author Pengtao Qiu
 */
public class SelfSignedCredentialOpenJdkSSLContextFactory extends AbstractOpenJdkSecureEngineFactory {

    private SSLContext sslContext;

    public SelfSignedCredentialOpenJdkSSLContextFactory() {
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
