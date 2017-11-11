package com.firefly.net.tcp.secure.openssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
 * An insecure {@link TrustManagerFactory} that trusts all X.509 certificates without any verification.
 * <p>
 * <strong>NOTE:</strong>
 * Never use this {@link TrustManagerFactory} in production.
 * It is purely for testing purposes, and thus it is very insecure.
 * </p>
 */
public final class InsecureTrustManagerFactory extends SimpleTrustManagerFactory {

    private static final Logger logger = LoggerFactory.getLogger("firefly-system");

    public static final TrustManagerFactory INSTANCE = new InsecureTrustManagerFactory();

    private static final TrustManager tm = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s) {
            logger.debug("Accepting a client certificate: " + chain[0].getSubjectDN());
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s) {
            logger.debug("Accepting a server certificate: " + chain[0].getSubjectDN());
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return EmptyArrays.EMPTY_X509_CERTIFICATES;
        }
    };

    private InsecureTrustManagerFactory() {
    }

    @Override
    protected void engineInit(KeyStore keyStore) throws Exception {
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception {
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return new TrustManager[]{tm};
    }
}
