package com.firefly.net.tcp.secure;

import com.firefly.net.ApplicationProtocolSelector;
import com.firefly.net.SecureSessionHandshakeListener;
import com.firefly.net.Session;
import com.firefly.utils.io.BufferUtils;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public class OpenSSLSession extends AbstractSecureSession {
    public OpenSSLSession(Session session, SSLEngine sslEngine,
                          ApplicationProtocolSelector applicationProtocolSelector,
                          SecureSessionHandshakeListener handshakeListener) throws IOException {
        super(session, sslEngine, applicationProtocolSelector, handshakeListener);
    }

    @Override
    protected SSLEngineResult unwrap(ByteBuffer input) throws IOException {
        ByteBuffer tmp = BufferUtils.toDirectBuffer(input);
        SSLEngineResult result = sslEngine.unwrap(tmp, receivedAppBuf);
        int consumed = result.bytesConsumed();
        if (input != receivedPacketBuf) {
            receivedPacketBuf.position(receivedPacketBuf.position() + consumed);
        }
        if (tmp != input) {
            input.position(input.position() + consumed);
        }
        return result;
    }

    @Override
    protected SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws IOException {
        ByteBuffer tmp = BufferUtils.toDirectBuffer(src);
        return sslEngine.wrap(tmp, dst);
    }
}
