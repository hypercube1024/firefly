package com.firefly.net.tcp;

import com.firefly.net.Session;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;


public abstract class AbstractTcpConnection implements TcpConnection {

    public static final String DEFAULT_CHARSET = "UTF-8";

    protected final Session session;
    protected final List<Action0> closeListeners = new LinkedList<>();
    protected final List<Action1<Throwable>> exceptionListeners = new LinkedList<>();
    protected Action1<ByteBuffer> buffer;
    protected volatile Object attachment;

    public AbstractTcpConnection(Session session) {
        this.session = session;
    }

    @Override
    public TcpConnection receive(Action1<ByteBuffer> buffer) {
        this.buffer = buffer;
        return this;
    }

    @Override
    public TcpConnection onException(Action1<Throwable> exception) {
        exceptionListeners.add(exception);
        return this;
    }

    @Override
    public TcpConnection onClose(Action0 closeCallback) {
        closeListeners.add(closeCallback);
        return this;
    }

    public void notifyClose() {
        closeListeners.forEach(Action0::call);
    }

    public void notifyException(Throwable t) {
        exceptionListeners.forEach(e -> e.call(t));
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
    public int getSessionId() {
        return session.getSessionId();
    }

    @Override
    public long getOpenTime() {
        return session.getOpenTime();
    }

    @Override
    public long getCloseTime() {
        return session.getCloseTime();
    }

    @Override
    public long getDuration() {
        return session.getDuration();
    }

    @Override
    public long getLastReadTime() {
        return session.getLastReadTime();
    }

    @Override
    public long getLastWrittenTime() {
        return session.getLastWrittenTime();
    }

    @Override
    public long getLastActiveTime() {
        return session.getLastActiveTime();
    }

    @Override
    public long getReadBytes() {
        return session.getReadBytes();
    }

    @Override
    public long getWrittenBytes() {
        return session.getWrittenBytes();
    }

    @Override
    public void close() {
        session.close();
    }

    @Override
    public void closeNow() {
        session.closeNow();
    }

    @Override
    public void shutdownOutput() {
        session.shutdownOutput();
    }

    @Override
    public void shutdownInput() {
        session.shutdownInput();
    }

    @Override
    public boolean isOpen() {
        return session.isOpen();
    }

    @Override
    public boolean isClosed() {
        return session.isClosed();
    }

    @Override
    public boolean isShutdownOutput() {
        return session.isShutdownOutput();
    }

    @Override
    public boolean isShutdownInput() {
        return session.isShutdownInput();
    }

    @Override
    public boolean isWaitingForClose() {
        return session.isWaitingForClose();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return session.getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return session.getRemoteAddress();
    }

    @Override
    public long getIdleTimeout() {
        return session.getIdleTimeout();
    }

    @Override
    public long getMaxIdleTimeout() {
        return session.getMaxIdleTimeout();
    }
}
