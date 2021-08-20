package com.fireflysource.net.tcp.secure.wildfly;

import com.fireflysource.net.tcp.secure.utils.SecureUtils;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

import static com.fireflysource.net.tcp.secure.utils.SecureUtils.*;

public class SelfSignedCertificateWildflySSLContextFactory extends AbstractWildflySecureEngineFactory {

    private SSLContext sslContext;

    public SelfSignedCertificateWildflySSLContextFactory() {
        try (InputStream in = SecureUtils.getSelfSignedCertificate()) {
            sslContext = getSSLContext(in, SELF_SIGNED_KEY_STORE_PASSWORD, SELF_SIGNED_KEY_PASSWORD, SELF_SIGNED_KEY_STORE_TYPE);
        } catch (Throwable e) {
            LOG.error("get SSL context error", e);
        }
    }

    @Override
    public SSLContext getSSLContext() {
        return sslContext;
    }
}
