package com.fireflysource.net.tcp.secure.conscrypt;

import com.fireflysource.common.sys.CommonLogger;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;
import org.conscrypt.Conscrypt;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractConscryptSecureEngineFactory implements SecureEngineFactory {

    private static final CommonLogger log = CommonLogger.create(AbstractConscryptSecureEngineFactory.class);

    private static String provideName;

    static {
        Provider provider = Conscrypt.newProvider();
        provideName = provider.getName();
        Security.addProvider(provider);
        log.info(() -> "add Conscrypt security provider");
    }

    public static String getProvideName() {
        return provideName;
    }

    public SSLContext getSSLContextWithManager(KeyManager[] km, TrustManager[] tm, SecureRandom random)
            throws NoSuchAlgorithmException, KeyManagementException, NoSuchProviderException {
        long start = System.currentTimeMillis();
        final SSLContext sslContext = SSLContext.getInstance("TLSv1.2", provideName);
        sslContext.init(km, tm, random);
        long end = System.currentTimeMillis();
        log.info(() -> "creating Conscrypt SSL context spends " + (end - start) + "ms");
        return sslContext;
    }

    public SSLContext getSSLContext(InputStream in, String keystorePassword, String keyPassword)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableKeyException, KeyManagementException, NoSuchProviderException {
        return getSSLContext(in, keystorePassword, keyPassword, null, null, null);
    }

    public SSLContext getSSLContext(InputStream in, String keystorePassword, String keyPassword,
                                    String keyManagerFactoryType, String trustManagerFactoryType, String sslProtocol)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableKeyException, KeyManagementException, NoSuchProviderException {
        long start = System.currentTimeMillis();
        final SSLContext sslContext;

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(in, keystorePassword != null ? keystorePassword.toCharArray() : null);

        // PKIX,SunX509
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyManagerFactoryType == null ? "SunX509" : keyManagerFactoryType);
        kmf.init(ks, keyPassword != null ? keyPassword.toCharArray() : null);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(trustManagerFactoryType == null ? "SunX509" : trustManagerFactoryType);
        tmf.init(ks);

        // TLSv1 TLSv1.2
        sslContext = SSLContext.getInstance(sslProtocol == null ? "TLSv1.2" : sslProtocol, provideName);
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        long end = System.currentTimeMillis();
        log.info(() -> "creating Conscrypt SSL context spends " + (end - start) + "ms");
        return sslContext;
    }

    abstract public SSLContext getSSLContext();
}
