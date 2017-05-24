package com.firefly.net.tcp;

import com.firefly.net.Decoder;
import com.firefly.net.Handler;
import com.firefly.net.Session;

import java.nio.ByteBuffer;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractSimpleHandler implements Handler {

    static final Decoder decoder = (ByteBuffer buf, Session session) -> {
        Object o = session.getAttachment();
        if (o != null) {
            TcpConnectionImpl c = (TcpConnectionImpl) o;
            if (c.buffer != null) {
                c.buffer.call(buf);
            }
        }
    };

    static final Decoder sslDecoder = (ByteBuffer buf, Session session) -> {
        Object o = session.getAttachment();
        if (o != null && o instanceof SecureTcpConnectionImpl) {
            SecureTcpConnectionImpl c = (SecureTcpConnectionImpl) o;
            ByteBuffer plaintext = c.sslSession.read(buf);
            if (plaintext != null && c.sslSession.isHandshakeFinished()) {
                if (c.buffer != null) {
                    c.buffer.call(plaintext);
                }
            }
        }
    };


    @Override
    public void sessionClosed(Session session) throws Throwable {
        Object o = session.getAttachment();
        if (o != null && o instanceof AbstractTcpConnection) {
            AbstractTcpConnection c = (AbstractTcpConnection) o;
            if (c.closeCallback != null) {
                c.closeCallback.call();
            }
        }
    }

    @Override
    public void messageReceived(Session session, Object message) throws Throwable {
    }

    @Override
    public void exceptionCaught(Session session, Throwable t) throws Throwable {
        Object o = session.getAttachment();
        if (o != null && o instanceof AbstractTcpConnection) {
            AbstractTcpConnection c = (AbstractTcpConnection) o;
            if (c.exception != null) {
                c.exception.call(t);
            }
        }
    }

    protected void sslSessionClosed(Session session) throws Throwable {
        try {
            sessionClosed(session);
        } finally {
            Object o = session.getAttachment();
            if (o != null && o instanceof SecureTcpConnectionImpl) {
                SecureTcpConnectionImpl c = (SecureTcpConnectionImpl) o;
                c.sslSession.close();
            }
        }
    }

}
