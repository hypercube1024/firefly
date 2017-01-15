package com.firefly.net.tcp.ssl;

import com.firefly.net.SSLContextFactory;
import com.firefly.utils.time.Millisecond100Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * @author Pengtao Qiu
 */
public abstract class AbstractJDKSSLContextFactory implements SSLContextFactory {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected volatile SSLContext sslContext;

    @Override
    public SSLEngine createSSLEngine(boolean clientMode) {
        SSLEngine sslEngine = getSslContext(clientMode).createSSLEngine();
        sslEngine.setUseClientMode(clientMode);
        return sslEngine;
    }

    public SSLContext getSslContext(boolean clientMode) {
        init(clientMode);
        return sslContext;
    }

    private void init(boolean clientMode) {
        if (sslContext == null) {
            synchronized (this) {
                if (sslContext == null) {
                    sslContext = createSSLContext(clientMode);
                }
            }
        }
    }

    public SSLContext getSSLContextWithManager(KeyManager[] km, TrustManager[] tm, SecureRandom random)
            throws NoSuchAlgorithmException, KeyManagementException {
        long start = Millisecond100Clock.currentTimeMillis();
        final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(km, tm, random);
        long end = Millisecond100Clock.currentTimeMillis();
        log.info("creating SSL context spends {} ms", (end - start));
        return sslContext;
    }

    public SSLContext getSSLContext(InputStream in, String keystorePassword, String keyPassword)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableKeyException, KeyManagementException {
        return getSSLContext(in, keystorePassword, keyPassword, null, null, null);
    }

    public SSLContext getSSLContext(InputStream in, String keystorePassword, String keyPassword,
                                    String keyManagerFactoryType, String trustManagerFactoryType, String sslProtocol)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableKeyException, KeyManagementException {
        long start = Millisecond100Clock.currentTimeMillis();
        final SSLContext sslContext;

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(in, keystorePassword != null ? keystorePassword.toCharArray() : null);

        // PKIX,SunX509
        KeyManagerFactory kmf = KeyManagerFactory
                .getInstance(keyManagerFactoryType == null ? "SunX509" : keyManagerFactoryType);
        kmf.init(ks, keyPassword != null ? keyPassword.toCharArray() : null);

        TrustManagerFactory tmf = TrustManagerFactory
                .getInstance(trustManagerFactoryType == null ? "SunX509" : trustManagerFactoryType);
        tmf.init(ks);

        // TLSv1 TLSv1.2
        sslContext = SSLContext.getInstance(sslProtocol == null ? "TLSv1.2" : sslProtocol);
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        long end = Millisecond100Clock.currentTimeMillis();
        log.info("creating SSL context spends time in {} ms", (end - start));
        return sslContext;
    }

    abstract public SSLContext createSSLContext(boolean clientMode);
}
