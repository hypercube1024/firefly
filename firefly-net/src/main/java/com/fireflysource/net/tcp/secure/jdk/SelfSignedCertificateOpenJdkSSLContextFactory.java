package com.fireflysource.net.tcp.secure.jdk;


import com.fireflysource.net.tcp.secure.utils.SecureUtils;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

/**
 * @author Pengtao Qiu
 */
public class SelfSignedCertificateOpenJdkSSLContextFactory extends AbstractOpenJdkSecureEngineFactory {

    private SSLContext sslContext;

    public SelfSignedCertificateOpenJdkSSLContextFactory() {
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
