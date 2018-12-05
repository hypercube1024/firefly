package com.fireflysource.net.tcp.secure.conscrypt;

import com.fireflysource.net.tcp.TcpConnection;
import com.fireflysource.net.tcp.secure.AbstractSecureEngine;
import com.fireflysource.net.tcp.secure.ApplicationProtocolSelector;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
public class ConscryptSecureEngine extends AbstractSecureEngine {

    public ConscryptSecureEngine(TcpConnection tcpConnection, SSLEngine sslEngine,
                                 ApplicationProtocolSelector applicationProtocolSelector) {
        super(tcpConnection, sslEngine, applicationProtocolSelector);
    }

    @Override
    protected SSLEngineResult unwrap(ByteBuffer input) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Session {} read data, src -> {}, dst -> {}",
                    tcpConnection.getId(), input.isDirect(), receivedAppBuf.isDirect());
        }
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
