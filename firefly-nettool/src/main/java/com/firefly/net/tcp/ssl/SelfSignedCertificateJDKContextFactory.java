package com.firefly.net.tcp.ssl;

import javax.net.ssl.SSLContext;

/**
 * @author Pengtao Qiu
 */
public class SelfSignedCertificateJDKContextFactory extends AbstractJDKSSLContextFactory {
    @Override
    public SSLContext createSSLContext(boolean clientMode) {
        return null;
    }
}
