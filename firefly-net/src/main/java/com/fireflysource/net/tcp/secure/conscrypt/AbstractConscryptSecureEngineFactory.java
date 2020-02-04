package com.fireflysource.net.tcp.secure.conscrypt;

import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.tcp.TcpConnection;
import com.fireflysource.net.tcp.secure.SecureEngine;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;
import org.conscrypt.Conscrypt;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractConscryptSecureEngineFactory implements SecureEngineFactory {

    protected static final LazyLogger LOG = SystemLogger.create(AbstractConscryptSecureEngineFactory.class);
    public static final String SECURE_PROTOCOL = "TLSv1.3";

    private static String provideName;

    static {
        Provider provider = Conscrypt.newProvider();
        provideName = provider.getName();
        Security.addProvider(provider);
        LOG.info(() -> "add Conscrypt security provider");
    }

    public static String getProvideName() {
        return provideName;
    }

    public SSLContext getSSLContextWithManager(KeyManager[] km, TrustManager[] tm, SecureRandom random)
            throws NoSuchAlgorithmException, KeyManagementException, NoSuchProviderException {
        long start = System.currentTimeMillis();

        final SSLContext sslContext = SSLContext.getInstance(SECURE_PROTOCOL, provideName);
        sslContext.init(km, tm, random);

        long end = System.currentTimeMillis();
        String protocol = sslContext.getProtocol();
        LOG.info(() -> "Creating Conscrypt SSL context. time: " + (end - start) + "ms, protocol: " + protocol);
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

        sslContext = SSLContext.getInstance(sslProtocol == null ? SECURE_PROTOCOL : sslProtocol, provideName);
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        long end = System.currentTimeMillis();
        String protocol = sslContext.getProtocol();
        LOG.info(() -> "Creating Conscrypt SSL context. time: " + (end - start) + "ms, protocol: " + protocol);
        return sslContext;
    }

    @Override
    public SecureEngine create(TcpConnection connection, boolean clientMode, List<String> supportedProtocols) {
        SSLEngine sslEngine = getSSLContext().createSSLEngine();
        sslEngine.setUseClientMode(clientMode);
        ConscryptApplicationProtocolSelector selector = new ConscryptApplicationProtocolSelector(sslEngine, supportedProtocols);
        return new ConscryptSecureEngine(connection, sslEngine, selector);
    }

    public SecureEngine create(TcpConnection connection, boolean clientMode, String peerHost, int peerPort,
                               List<String> supportedProtocols) {
        SSLEngine sslEngine = getSSLContext().createSSLEngine(peerHost, peerPort);
        sslEngine.setUseClientMode(clientMode);
        ConscryptApplicationProtocolSelector selector = new ConscryptApplicationProtocolSelector(sslEngine, supportedProtocols);
        return new ConscryptSecureEngine(connection, sslEngine, selector);
    }

    abstract public SSLContext getSSLContext();
}
