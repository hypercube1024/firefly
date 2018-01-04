package com.firefly.codec.http2.stream;

import com.firefly.codec.common.AbstractConnection;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.net.SecureSession;
import com.firefly.net.Session;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;

abstract public class AbstractHTTPConnection extends AbstractConnection implements HTTPConnection {

    protected final HttpVersion httpVersion;
    protected volatile Object attachment;
    protected Action1<HTTPConnection> closedListener;
    protected Action2<HTTPConnection, Throwable> exceptionListener;

    public AbstractHTTPConnection(SecureSession secureSession, Session tcpSession, HttpVersion httpVersion) {
        super(secureSession, tcpSession);
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
    public boolean isEncrypted() {
        return secureSession != null;
    }

    @Override
    public HTTPConnection onClose(Action1<HTTPConnection> closedListener) {
        this.closedListener = closedListener;
        return this;
    }

    @Override
    public HTTPConnection onException(Action2<HTTPConnection, Throwable> exceptionListener) {
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
