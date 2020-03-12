package com.fireflysource.net.tcp.secure.jdk;


import com.fireflysource.net.tcp.secure.utils.SecureUtils;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;

/**
 * @author Pengtao Qiu
 */
public class DefaultCredentialOpenJdkSSLContextFactory extends AbstractOpenJdkSecureEngineFactory {

    private SSLContext sslContext;

    public DefaultCredentialOpenJdkSSLContextFactory() {
        try {
            sslContext = getSSLContext(new ByteArrayInputStream(SecureUtils.DEFAULT_CREDENTIAL), "ptmima1234", "ptmima4321");
        } catch (Throwable e) {
            LOG.error(e, () -> "get SSL context error");
        }
    }

    @Override
    public SSLContext getSSLContext() {
        return sslContext;
    }
}
