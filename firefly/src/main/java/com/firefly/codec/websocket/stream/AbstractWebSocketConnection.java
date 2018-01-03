package com.firefly.codec.websocket.stream;

import com.firefly.codec.websocket.model.IncomingFrames;
import com.firefly.net.SecureSession;
import com.firefly.net.Session;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;
import com.firefly.utils.io.IO;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractWebSocketConnection implements WebSocketConnection {

    protected final SecureSession secureSession;
    protected final Session tcpSession;
    protected volatile Object attachment;
    protected Action1<WebSocketConnection> closedListener;
    protected Action2<WebSocketConnection, Throwable> exceptionListener;
    protected IncomingFrames incomingFrames;

    public AbstractWebSocketConnection(SecureSession secureSession, Session tcpSession, IncomingFrames incomingFrames) {
        this.secureSession = secureSession;
        this.tcpSession = tcpSession;
        this.incomingFrames = incomingFrames;
    }

    @Override
    public WebSocketConnection onClose(Action1<WebSocketConnection> closedListener) {
        this.closedListener = closedListener;
        return this;
    }

    @Override
    public WebSocketConnection onException(Action2<WebSocketConnection, Throwable> exceptionListener) {
        this.exceptionListener = exceptionListener;
        return this;
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
    public void setIncomingFrames(IncomingFrames incomingFrames) {
        this.incomingFrames = incomingFrames;
    }

    @Override
    public IncomingFrames getIncomingFrames() {
        return incomingFrames;
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
    public boolean isOpen() {
        return tcpSession.isOpen();
    }

    @Override
    public boolean isClosed() {
        return tcpSession.isClosed();
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
