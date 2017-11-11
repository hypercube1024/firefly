package com.firefly.net.tcp.secure;

import com.firefly.net.*;
import com.firefly.net.tcp.secure.openssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.io.IOException;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractOpenSSLSecureSessionFactory implements SecureSessionFactory {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    protected volatile SslContext sslContext;

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

    abstract public SslContext createSSLContext(boolean clientMode);
}
