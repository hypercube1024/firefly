package com.firefly.net.tcp;

import com.firefly.net.Config;
import com.firefly.net.SSLContextFactory;
import com.firefly.net.tcp.ssl.SelfSignedCertificateOpenSSLContextFactory;

public class TcpConfiguration extends Config {

    // SSL/TLS settings
    private boolean isSecureConnectionEnabled;
    private SSLContextFactory sslContextFactory = new SelfSignedCertificateOpenSSLContextFactory();

    public boolean isSecureConnectionEnabled() {
        return isSecureConnectionEnabled;
    }

    public void setSecureConnectionEnabled(boolean isSecureConnectionEnabled) {
        this.isSecureConnectionEnabled = isSecureConnectionEnabled;
    }

    public SSLContextFactory getSslContextFactory() {
        return sslContextFactory;
    }

    public void setSslContextFactory(SSLContextFactory sslContextFactory) {
        this.sslContextFactory = sslContextFactory;
    }

}
