package com.firefly.server.http2;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.AbstractHTTPConnection;
import com.firefly.codec.http2.stream.ConnectionType;
import com.firefly.codec.http2.stream.HTTPTunnelConnection;
import com.firefly.net.SecureSession;
import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.function.Action1;

import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * @author Pengtao Qiu
 */
public class HTTP1ServerTunnelConnection extends AbstractHTTPConnection implements HTTPTunnelConnection {

    Action1<ByteBuffer> content;

    public HTTP1ServerTunnelConnection(SecureSession secureSession, com.firefly.net.Session tcpSession, HttpVersion httpVersion) {
        super(secureSession, tcpSession, httpVersion);
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
    public void receive(Action1<ByteBuffer> content) {
        this.content = content;
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.HTTP_TUNNEL;
    }
}
