package com.firefly.net;

import java.io.IOException;

/**
 * @author Pengtao Qiu
 */
public interface SecureSessionFactory {

    SecureSession create(Session session, boolean clientMode,
                         SecureSessionHandshakeListener secureSessionHandshakeListener) throws IOException;

    SecureSession create(Session session, boolean clientMode,
                         String peerHost, int peerPort,
                         SecureSessionHandshakeListener secureSessionHandshakeListener) throws IOException;

}
