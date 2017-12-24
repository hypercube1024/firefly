package com.firefly.net.tcp.codec.ffsocks.stream;

import com.firefly.net.tcp.TcpConnection;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractFfsocksConnection implements FfsocksConnection {

    protected TcpConnection tcpConnection;
    protected Stream stream;
    protected Session session;

    @Override
    public Object getAttachment() {
        return tcpConnection.getAttachment();
    }

    @Override
    public void setAttachment(Object object) {
        tcpConnection.setAttachment(object);
    }

    @Override
    public int getSessionId() {
        return tcpConnection.getSessionId();
    }

    @Override
    public long getOpenTime() {
        return tcpConnection.getOpenTime();
    }

    @Override
    public long getCloseTime() {
        return tcpConnection.getCloseTime();
    }

    @Override
    public long getDuration() {
        return tcpConnection.getDuration();
    }

    @Override
    public long getLastReadTime() {
        return tcpConnection.getLastReadTime();
    }

    @Override
    public long getLastWrittenTime() {
        return tcpConnection.getLastWrittenTime();
    }

    @Override
    public long getLastActiveTime() {
        return tcpConnection.getLastActiveTime();
    }

    @Override
    public long getReadBytes() {
        return tcpConnection.getReadBytes();
    }

    @Override
    public long getWrittenBytes() {
        return tcpConnection.getWrittenBytes();
    }

    @Override
    public long getIdleTimeout() {
        return tcpConnection.getIdleTimeout();
    }

    @Override
    public long getMaxIdleTimeout() {
        return tcpConnection.getMaxIdleTimeout();
    }

    @Override
    public boolean isOpen() {
        return tcpConnection.isOpen();
    }

    @Override
    public boolean isClosed() {
        return tcpConnection.isClosed();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return tcpConnection.getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return tcpConnection.getRemoteAddress();
    }

    @Override
    public void close() throws IOException {
        tcpConnection.close();
    }

    @Override
    public Stream getStream() {
        return stream;
    }

    @Override
    public Session getSession() {
        return session;
    }
}
