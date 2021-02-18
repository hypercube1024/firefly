package com.fireflysource.net.tcp.secure.utils;

import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.cert.X509Certificate;

/**
 * @author Pengtao Qiu
 */
abstract public class SecureUtils {

    public static final String SELF_SIGNED_KEY_STORE_TYPE = "jks";
    public static final String SELF_SIGNED_KEY_STORE_PASSWORD = "123456";
    public static final String SELF_SIGNED_KEY_PASSWORD = "654321";
    public static final String KEY_MANAGER_FACTORY_TYPE = "SunX509"; // // PKIX, SunX509
    public static final String TRUST_MANAGER_FACTORY_TYPE = "SunX509";

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
