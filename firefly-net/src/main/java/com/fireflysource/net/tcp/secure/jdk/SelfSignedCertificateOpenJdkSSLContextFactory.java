package com.fireflysource.net.tcp.secure.jdk;


import com.fireflysource.net.tcp.secure.utils.SecureUtils;

import javax.net.ssl.SSLContext;
import java.io.InputStream;

import static com.fireflysource.net.tcp.secure.utils.SecureUtils.*;

/**
 * @author Pengtao Qiu
 */
public class SelfSignedCertificateOpenJdkSSLContextFactory extends AbstractOpenJdkSecureEngineFactory {

    private SSLContext sslContext;

    public SelfSignedCertificateOpenJdkSSLContextFactory() {
        try (InputStream in = SecureUtils.getSelfSignedCertificate()) {
            sslContext = getSSLContext(in, SELF_SIGNED_KEY_STORE_PASSWORD, SELF_SIGNED_KEY_PASSWORD, SELF_SIGNED_KEY_STORE_TYPE);
        } catch (Throwable e) {
            LOG.error(e, () -> "get SSL context error");
        }
    }

    @Override
    public SSLContext getSSLContext() {
        return sslContext;
    }
}
