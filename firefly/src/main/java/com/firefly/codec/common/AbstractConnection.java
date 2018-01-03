package com.firefly.codec.common;

import com.firefly.net.Connection;
import com.firefly.net.SecureSession;
import com.firefly.net.Session;
import com.firefly.utils.io.IO;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractConnection implements Connection {

    protected final SecureSession secureSession;
    protected final Session tcpSession;
    protected volatile Object attachment;

    public AbstractConnection(SecureSession secureSession, Session tcpSession) {
        this.secureSession = secureSession;
        this.tcpSession = tcpSession;
    }

    @Override
    public int getSessionId() {
        return tcpSession.getSessionId();
    }

    @Override
    public long getOpenTime() {
        return tcpSession.getOpenTime();
    }

    @Override
    public long getCloseTime() {
        return tcpSession.getCloseTime();
    }

    @Override
    public long getDuration() {
        return tcpSession.getDuration();
    }

    @Override
    public long getLastReadTime() {
        return tcpSession.getLastReadTime();
    }

    @Override
    public long getLastWrittenTime() {
        return tcpSession.getLastWrittenTime();
    }

    @Override
    public long getLastActiveTime() {
        return tcpSession.getLastActiveTime();
    }

    @Override
    public long getReadBytes() {
        return tcpSession.getReadBytes();
    }

    @Override
    public long getWrittenBytes() {
        return tcpSession.getWrittenBytes();
    }

    @Override
    public boolean isOpen() {
        return tcpSession.isOpen();
    }

    @Override
    public boolean isClosed() {
        return tcpSession.isClosed();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return tcpSession.getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return tcpSession.getRemoteAddress();
    }

    @Override
    public long getIdleTimeout() {
        return tcpSession.getIdleTimeout();
    }

    @Override
    public long getMaxIdleTimeout() {
        return tcpSession.getMaxIdleTimeout();
    }

    @Override
    public Object getAttachment() {
        return attachment;
    }

    @Override
    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public void close() {
        Optional.ofNullable(secureSession)
                .filter(SecureSession::isOpen)
                .ifPresent(IO::close);

        Optional.ofNullable(tcpSession)
                .filter(Session::isOpen)
                .ifPresent(Session::close);

        attachment = null;
    }
}
