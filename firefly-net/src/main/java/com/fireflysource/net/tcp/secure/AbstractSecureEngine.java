package com.fireflysource.net.tcp.secure;

import com.fireflysource.common.sys.CommonLogger;
import com.fireflysource.net.tcp.TcpConnection;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractSecureEngine implements SecureEngine {

    private static final CommonLogger log = CommonLogger.create(AbstractSecureEngine.class);

    protected final SSLEngine sslEngine;
    protected final ApplicationProtocolSelector applicationProtocolSelector;
    protected final TcpConnection connection;

    protected ByteBuffer receivedPacketBuf;
    protected ByteBuffer receivedAppBuf;

    protected AtomicBoolean closed = new AtomicBoolean(false);
    protected SSLEngineResult.HandshakeStatus initialHSStatus;
    protected boolean initialHSComplete;

    protected AbstractSecureEngine(TcpConnection connection, SSLEngine sslEngine, ApplicationProtocolSelector applicationProtocolSelector) {
        this.connection = connection;
        this.sslEngine = sslEngine;
        this.applicationProtocolSelector = applicationProtocolSelector;

        receivedAppBuf = newBuffer(sslEngine.getSession().getApplicationBufferSize());
        initialHSComplete = false;
    }

    @Override
    public String getApplicationProtocol() {
        String protocol = applicationProtocolSelector.getApplicationProtocol();
        log.debug(() -> "selected protocol -> " + protocol);
        return protocol;
    }

    @Override
    public List<String> getSupportedApplicationProtocols() {
        return applicationProtocolSelector.getSupportedApplicationProtocols();
    }

    protected SSLEngineResult unwrap(ByteBuffer input) throws IOException {
        SSLEngineResult result = sslEngine.unwrap(input, receivedAppBuf);
        if (input != receivedPacketBuf) {
            int consumed = result.bytesConsumed();
            receivedPacketBuf.position(receivedPacketBuf.position() + consumed);
        }
        return result;
    }

    protected SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws IOException {
        return sslEngine.wrap(src, dst);
    }

    protected ByteBuffer newBuffer(int size) {
        return ByteBuffer.allocate(size);
    }
}
