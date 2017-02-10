package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;

import java.io.IOException;
import java.net.InetSocketAddress;

abstract public class AbstractHTTPConnection implements HTTPConnection {

    protected final SSLSession sslSession;
    protected final Session tcpSession;
    protected final HttpVersion httpVersion;
    protected volatile Object attachment;
    protected Action1<HTTPConnection> closedListener;
    protected Action2<HTTPConnection, Throwable> exceptionListener;

    public AbstractHTTPConnection(SSLSession sslSession, Session tcpSession, HttpVersion httpVersion) {
        this.sslSession = sslSession;
        this.tcpSession = tcpSession;
        this.httpVersion = httpVersion;
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
    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    @Override
    public boolean isOpen() {
        return tcpSession.isOpen();
    }

    @Override
    public void close() throws IOException {
        if (sslSession != null && sslSession.isOpen()) {
            sslSession.close();
        }
        if (tcpSession != null && tcpSession.isOpen()) {
            tcpSession.close();
        }
        attachment = null;
    }

    @Override
    public boolean isEncrypted() {
        return sslSession != null;
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
    public long getIdleTimeout() {
        return tcpSession.getIdleTimeout();
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
    public HTTPConnection close(Action1<HTTPConnection> closedListener) {
        this.closedListener = closedListener;
        return this;
    }

    @Override
    public HTTPConnection exception(Action2<HTTPConnection, Throwable> exceptionListener) {
        this.exceptionListener = exceptionListener;
        return this;
    }

    Action1<HTTPConnection> getClosedListener() {
        return closedListener;
    }

    Action2<HTTPConnection, Throwable> getExceptionListener() {
        return exceptionListener;
    }
}
