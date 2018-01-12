package com.firefly.net.tcp.secure.conscrypt;

import com.firefly.net.*;
import com.firefly.utils.lang.Pair;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public class ConscryptSecureSessionFactory implements SecureSessionFactory {

    private SSLContextFactory clientSSLContextFactory = new NoCheckConscryptSSLContextFactory();
    private SSLContextFactory serverSSLContextFactory = new DefaultCredentialConscryptSSLContextFactory();
    private List<String> supportedProtocols;

    public ConscryptSecureSessionFactory() {

    }

    public ConscryptSecureSessionFactory(SSLContextFactory clientSSLContextFactory, SSLContextFactory serverSSLContextFactory) {
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
        sslContextFactory.setSupportedProtocols(supportedProtocols);
        Pair<SSLEngine, ApplicationProtocolSelector> p = sslContextFactory.createSSLEngine(clientMode);
        return new ConscryptSSLSession(session, p.first, p.second, secureSessionHandshakeListener);
    }

    @Override
    public SecureSession create(Session session, boolean clientMode, String peerHost, int peerPort, SecureSessionHandshakeListener secureSessionHandshakeListener) throws IOException {
        SSLContextFactory sslContextFactory = from(clientMode);
        sslContextFactory.setSupportedProtocols(supportedProtocols);
        Pair<SSLEngine, ApplicationProtocolSelector> p = sslContextFactory.createSSLEngine(clientMode, peerHost, peerPort);
        return new ConscryptSSLSession(session, p.first, p.second, secureSessionHandshakeListener);
    }

    protected SSLContextFactory from(boolean clientMode) {
        return clientMode ? clientSSLContextFactory : serverSSLContextFactory;
    }

    @Override
    public List<String> getSupportedProtocols() {
        return supportedProtocols;
    }

    @Override
    public void setSupportedProtocols(List<String> supportedProtocols) {
        this.supportedProtocols = supportedProtocols;
    }
}
