package com.fireflysource.net.tcp.secure.conscrypt;


import com.fireflysource.net.tcp.secure.utils.SecureUtils;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

/**
 * @author Pengtao Qiu
 */
public class SelfSignedCertificateConscryptSSLContextFactory extends AbstractConscryptSecureEngineFactory {

    private SSLContext sslContext;

    public SelfSignedCertificateConscryptSSLContextFactory() {
        try (InputStream in = SecureUtils.getSelfSignedCertificate()) {
            sslContext = getSSLContext(in, "123456", "654321", "JKS");
        } catch (Throwable e) {
            LOG.error(e, () -> "get SSL context error");
        }
    }

    @Override
    public SSLContext getSSLContext() {
        return sslContext;
    }
}
