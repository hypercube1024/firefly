package com.firefly.net.tcp.secure;

import com.firefly.net.ApplicationProtocolSelector;
import com.firefly.net.SecureSessionHandshakeListener;
import com.firefly.net.Session;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractJdkSSLSession extends AbstractSecureSession {

    public AbstractJdkSSLSession(Session session, SSLEngine sslEngine,
                                 ApplicationProtocolSelector applicationProtocolSelector,
                                 SecureSessionHandshakeListener handshakeListener) throws IOException {
        super(session, sslEngine, applicationProtocolSelector, handshakeListener);
    }

    @Override
    protected SSLEngineResult unwrap(ByteBuffer input) throws IOException {
        SSLEngineResult result = sslEngine.unwrap(input, receivedAppBuf);
        if (input != receivedPacketBuf) {
            int consumed = result.bytesConsumed();
            receivedPacketBuf.position(receivedPacketBuf.position() + consumed);
        }
        return result;
    }

    @Override
    protected SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws IOException {
        return sslEngine.wrap(src, dst);
    }

    @Override
    protected ByteBuffer newBuffer(int size) {
        return ByteBuffer.allocate(size);
    }
}
