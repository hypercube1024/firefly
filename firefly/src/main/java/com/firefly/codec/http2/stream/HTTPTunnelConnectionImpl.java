package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.net.buffer.FileRegion;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.function.Action1;

import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * @author Pengtao Qiu
 */
public class HTTPTunnelConnectionImpl extends AbstractHTTPConnection implements HTTPTunnelConnection {

    Action1<ByteBuffer> content;

    public HTTPTunnelConnectionImpl(SSLSession sslSession, com.firefly.net.Session tcpSession, HttpVersion httpVersion) {
        super(sslSession, tcpSession, httpVersion);
    }

    @Override
    public void write(ByteBuffer byteBuffer, Callback callback) {
        tcpSession.write(byteBuffer, callback);
    }

    @Override
    public void write(ByteBuffer[] buffers, Callback callback) {
        tcpSession.write(buffers, callback);
    }

    @Override
    public void write(Collection<ByteBuffer> buffers, Callback callback) {
        tcpSession.write(buffers, callback);
    }

    @Override
    public void write(FileRegion file, Callback callback) {
        tcpSession.write(file, callback);
    }

    @Override
    public void content(Action1<ByteBuffer> content) {
        this.content = content;
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.HTTP_TUNNEL;
    }
}
