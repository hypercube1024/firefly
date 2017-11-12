package com.firefly.net.tcp.secure.openssl;

import com.firefly.net.*;
import com.firefly.net.tcp.secure.openssl.nativelib.*;
import com.firefly.utils.exception.CommonRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractOpenSSLSecureSessionFactory implements SecureSessionFactory {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected static final List<String> DEFAULT_SUPPORTED_PROTOCOLS = Arrays.asList("h2", "http/1.1");

    protected List<String> supportedProtocols = DEFAULT_SUPPORTED_PROTOCOLS;
    protected volatile SslContext sslContext;

    public AbstractOpenSSLSecureSessionFactory() {

    }

    public AbstractOpenSSLSecureSessionFactory(List<String> supportedProtocols) {
        this.supportedProtocols = supportedProtocols;
    }

    @Override
    public SecureSession create(Session session, boolean clientMode,
                                SecureSessionHandshakeListener secureSessionHandshakeListener) throws IOException {
        SSLEngine sslEngine = getSslContext(clientMode).newEngine();
        sslEngine.setUseClientMode(clientMode);
        ApplicationProtocolSelector applicationProtocolSelector = (ApplicationProtocolSelector) sslEngine;
        return new OpenSSLSession(session, sslEngine, applicationProtocolSelector, secureSessionHandshakeListener);
    }

    @Override
    public SecureSession create(Session session, boolean clientMode,
                                String peerHost, int peerPort,
                                SecureSessionHandshakeListener secureSessionHandshakeListener) throws IOException {
        SSLEngine sslEngine = getSslContext(clientMode).newEngine(peerHost, peerPort);
        sslEngine.setUseClientMode(clientMode);
        ApplicationProtocolSelector applicationProtocolSelector = (ApplicationProtocolSelector) sslEngine;
        return new OpenSSLSession(session, sslEngine, applicationProtocolSelector, secureSessionHandshakeListener);
    }

    public SslContext createSSLContext(boolean clientMode) {
        SslContextBuilder sslContextBuilder = clientMode
                ? SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
                : SslContextBuilder.forServer(getCertificate(), getPrivateKey());

        try {
            return sslContextBuilder.ciphers(SecurityUtils.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                                    .applicationProtocolConfig(new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                                            ApplicationProtocolConfig.SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                                            supportedProtocols)).build();
        } catch (SSLException e) {
            log.error("create ssl context exception", e);
            throw new CommonRuntimeException(e);
        }
    }

    public List<String> getSupportedProtocols() {
        return supportedProtocols;
    }

    public void setSupportedProtocols(List<String> supportedProtocols) {
        this.supportedProtocols = supportedProtocols;
    }

    public SslContext getSslContext(boolean clientMode) {
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

    abstract public File getCertificate();

    abstract public File getPrivateKey();
}
