package com.firefly.net.tcp.secure.jdk;

import com.firefly.net.ApplicationProtocolSelector;
import com.firefly.net.SecureSessionHandshakeListener;
import com.firefly.net.Session;
import com.firefly.net.tcp.secure.AbstractJdkSSLSession;

import javax.net.ssl.SSLEngine;
import java.io.IOException;

/**
 * @author Pengtao Qiu
 */
public class JdkSSLSession extends AbstractJdkSSLSession {

    public JdkSSLSession(Session session, SSLEngine sslEngine,
                         ApplicationProtocolSelector applicationProtocolSelector,
                         SecureSessionHandshakeListener handshakeListener) throws IOException {
        super(session, sslEngine, applicationProtocolSelector, handshakeListener);
    }
}
