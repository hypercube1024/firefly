package com.firefly.net.tcp.secure.jdk;

import com.firefly.net.*;
import com.firefly.utils.lang.Pair;

import javax.net.ssl.SSLEngine;
import java.io.IOException;

/**
 * @author Pengtao Qiu
 */
public class JdkSecureSessionFactory implements SecureSessionFactory {

    private SSLContextFactory clientSSLContextFactory = new NoCheckJdkSSLContextFactory();
    private SSLContextFactory serverSSLContextFactory = new DefaultCredentialJdkSSLContextFactory();

    public JdkSecureSessionFactory() {
    }

    public JdkSecureSessionFactory(SSLContextFactory clientSSLContextFactory, SSLContextFactory serverSSLContextFactory) {
        this.clientSSLContextFactory = clientSSLContextFactory;
        this.serverSSLContextFactory = serverSSLContextFactory;
    }

    public SSLContextFactory getClientSSLContextFactory() {
        return clientSSLContextFactory;
    }

    public void setClientSSLContextFactory(SSLContextFactory clientSSLContextFactory) {
        this.clientSSLContextFactory = clientSSLContextFactory;
    }

    public SSLContextFactory getServerSSLContextFactory() {
        return serverSSLContextFactory;
    }

    public void setServerSSLContextFactory(SSLContextFactory serverSSLContextFactory) {
        this.serverSSLContextFactory = serverSSLContextFactory;
    }

    @Override
    public SecureSession create(Session session, boolean clientMode, SecureSessionHandshakeListener secureSessionHandshakeListener) throws IOException {
        SSLContextFactory sslContextFactory = from(clientMode);
        Pair<SSLEngine, ApplicationProtocolSelector> p = sslContextFactory.createSSLEngine(clientMode);
        return new JdkSSLSession(session, p.first, p.second, secureSessionHandshakeListener);
    }

    @Override
    public SecureSession create(Session session, boolean clientMode, String peerHost, int peerPort, SecureSessionHandshakeListener secureSessionHandshakeListener) throws IOException {
        SSLContextFactory sslContextFactory = from(clientMode);
        Pair<SSLEngine, ApplicationProtocolSelector> p = sslContextFactory.createSSLEngine(clientMode, peerHost, peerPort);
        return new JdkSSLSession(session, p.first, p.second, secureSessionHandshakeListener);
    }

    protected SSLContextFactory from(boolean clientMode) {
        return clientMode ? clientSSLContextFactory : serverSSLContextFactory;
    }
}
