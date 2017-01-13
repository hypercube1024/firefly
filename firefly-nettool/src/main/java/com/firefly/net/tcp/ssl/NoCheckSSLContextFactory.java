package com.firefly.net.tcp.ssl;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class NoCheckSSLContextFactory extends AbstractSSLContextFactory {

    @Override
    public void createSSLContext() {
        try {
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            sslContext = getSSLContextWithManager(null, new TrustManager[]{tm}, null);
        } catch (Throwable e) {
            log.error("get SSL context error", e);
        }
    }
}
