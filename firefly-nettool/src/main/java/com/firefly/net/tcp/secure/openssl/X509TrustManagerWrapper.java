package com.firefly.net.tcp.secure.openssl;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static com.firefly.net.tcp.secure.openssl.ObjectUtil.checkNotNull;

final class X509TrustManagerWrapper extends X509ExtendedTrustManager {

    private final X509TrustManager delegate;

    X509TrustManagerWrapper(X509TrustManager delegate) {
        this.delegate = checkNotNull(delegate, "delegate");
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String s) throws CertificateException {
        delegate.checkClientTrusted(chain, s);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String s, Socket socket)
            throws CertificateException {
        delegate.checkClientTrusted(chain, s);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String s, SSLEngine sslEngine)
            throws CertificateException {
        delegate.checkClientTrusted(chain, s);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String s) throws CertificateException {
        delegate.checkServerTrusted(chain, s);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String s, Socket socket)
            throws CertificateException {
        delegate.checkServerTrusted(chain, s);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String s, SSLEngine sslEngine)
            throws CertificateException {
        delegate.checkServerTrusted(chain, s);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }
}
