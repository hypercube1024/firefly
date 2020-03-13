package com.fireflysource.net.tcp.secure.utils;

import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.cert.X509Certificate;

/**
 * @author Pengtao Qiu
 */
abstract public class SecureUtils {

    public static InputStream getSelfSignedCertificate() {
        return SecureUtils.class.getClassLoader().getResourceAsStream("fireflyKeystore.jks");
    }

    public static X509TrustManager createX509TrustManagerNoCheck() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
    }
}
