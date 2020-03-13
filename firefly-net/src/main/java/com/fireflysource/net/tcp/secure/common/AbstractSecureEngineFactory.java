package com.fireflysource.net.tcp.secure.common;

import com.fireflysource.common.coroutine.CoroutineDispatchers;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;
import com.fireflysource.net.tcp.secure.SecureEngine;
import com.fireflysource.net.tcp.secure.SecureEngineFactory;
import kotlinx.coroutines.CoroutineScope;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Optional;

abstract public class AbstractSecureEngineFactory implements SecureEngineFactory {

    protected static final LazyLogger LOG = SystemLogger.create(AbstractSecureEngineFactory.class);

    public SSLContext getSSLContextWithManager(KeyManager[] km, TrustManager[] tm, SecureRandom random)
            throws NoSuchAlgorithmException, KeyManagementException, NoSuchProviderException {
        long start = System.currentTimeMillis();

        final SSLContext sslContext = SSLContext.getInstance(getSecureProtocol(), getProviderName());
        sslContext.init(km, tm, random);

        long end = System.currentTimeMillis();
        String protocol = sslContext.getProtocol();
        LOG.info(() -> "Creating SSL context. time: " + (end - start) + "ms, protocol: " + protocol);
        return sslContext;
    }

    public SSLContext getSSLContext(InputStream in, String keystorePassword, String keyPassword, String keyStoreType)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableKeyException, KeyManagementException, NoSuchProviderException {
        return getSSLContext(in, keystorePassword, keyPassword, keyStoreType, null, null, null);
    }

    public SSLContext getSSLContext(InputStream in, String keystorePassword, String keyPassword,
                                    String keyStoreType,
                                    String keyManagerFactoryType, String trustManagerFactoryType, String sslProtocol)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableKeyException, KeyManagementException, NoSuchProviderException {
        long start = System.currentTimeMillis();
        final SSLContext sslContext;

        KeyStore ks = KeyStore.getInstance(keyStoreType);
        ks.load(in, keystorePassword != null ? keystorePassword.toCharArray() : null);

        // PKIX, SunX509
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyManagerFactoryType == null ? "SunX509" : keyManagerFactoryType);
        kmf.init(ks, keyPassword != null ? keyPassword.toCharArray() : null);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(trustManagerFactoryType == null ? "SunX509" : trustManagerFactoryType);
        tmf.init(ks);

        sslContext = SSLContext.getInstance(sslProtocol == null ? getSecureProtocol() : sslProtocol, getProviderName());
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        long end = System.currentTimeMillis();
        String protocol = sslContext.getProtocol();
        LOG.info(() -> "Creating SSL context. time: " + (end - start) + "ms, protocol: " + protocol);
        return sslContext;
    }

    @Override
    public SecureEngine create(CoroutineScope coroutineScope, boolean clientMode, List<String> supportedProtocols) {
        SSLEngine sslEngine = getSSLContext().createSSLEngine();
        sslEngine.setUseClientMode(clientMode);
        ApplicationProtocolSelector selector = createApplicationProtocolSelector(sslEngine, supportedProtocols);
        CoroutineScope scope = Optional.ofNullable(coroutineScope).orElseGet(CoroutineDispatchers.INSTANCE::getComputationScope);
        return createSecureEngine(scope, sslEngine, selector);
    }

    @Override
    public SecureEngine create(CoroutineScope coroutineScope, boolean clientMode, String peerHost, int peerPort,
                               List<String> supportedProtocols) {
        SSLEngine sslEngine = getSSLContext().createSSLEngine(peerHost, peerPort);
        sslEngine.setUseClientMode(clientMode);
        ApplicationProtocolSelector selector = createApplicationProtocolSelector(sslEngine, supportedProtocols);
        CoroutineScope scope = Optional.ofNullable(coroutineScope).orElseGet(CoroutineDispatchers.INSTANCE::getComputationScope);
        return createSecureEngine(scope, sslEngine, selector);
    }

    abstract public SSLContext getSSLContext();

    abstract public String getSecureProtocol();

    abstract public String getProviderName();

    abstract public SecureEngine createSecureEngine(
            CoroutineScope coroutineScope,
            SSLEngine sslEngine,
            ApplicationProtocolSelector applicationProtocolSelector);

    abstract public ApplicationProtocolSelector createApplicationProtocolSelector(
            SSLEngine sslEngine, List<String> supportedProtocolList);
}
