package com.firefly.net;

import java.io.IOException;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface SecureSessionFactory {

    SecureSession create(Session session, boolean clientMode,
                         SecureSessionHandshakeListener secureSessionHandshakeListener) throws IOException;

    SecureSession create(Session session, boolean clientMode,
                         String peerHost, int peerPort,
                         SecureSessionHandshakeListener secureSessionHandshakeListener) throws IOException;

    void setSupportedProtocols(List<String> supportedProtocols);

    List<String> getSupportedProtocols();

}
