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
public class JdkSSLSession extends AbstractSecureSession {

    public JdkSSLSession(Session session, SSLEngine sslEngine,
                         ApplicationProtocolSelector applicationProtocolSelector,
                         SecureSessionHandshakeListener handshakeListener) throws IOException {
        super(session, sslEngine, applicationProtocolSelector, handshakeListener);
    }

    @Override
    protected SSLEngineResult unwrap(ByteBuffer input) throws IOException {
        SSLEngineResult result = sslEngine.unwrap(input, outAppBuffer);
        if (input != inNetBuffer) {
            int consumed = result.bytesConsumed();
            inNetBuffer.position(inNetBuffer.position() + consumed);
        }
        return result;
    }

    @Override
    protected SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws IOException {
        return sslEngine.wrap(src, dst);
    }

}
