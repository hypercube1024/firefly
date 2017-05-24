package com.firefly.codec.http2.stream;

import com.firefly.net.Handler;
import com.firefly.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHTTPHandler implements Handler {

    protected static Logger log = LoggerFactory.getLogger("firefly-system");

    protected final HTTP2Configuration config;

    public AbstractHTTPHandler(HTTP2Configuration config) {
        this.config = config;
    }

    @Override
    public void messageReceived(Session session, Object message) throws Throwable {
    }

    @Override
    public void exceptionCaught(Session session, Throwable t) throws Throwable {
        log.error("HTTP handler exception", t);
        if (session.getAttachment() instanceof AbstractHTTPConnection) {
            AbstractHTTPConnection httpConnection = (AbstractHTTPConnection) session.getAttachment();
            if (httpConnection != null) {
                if (httpConnection.getExceptionListener() != null) {
                    try {
                        httpConnection.getExceptionListener().call(httpConnection, t);
                    } catch (Throwable t1) {
                        log.error("http connection exception listener error", t1);
                    }
                }
                if (httpConnection.isOpen()) {
                    httpConnection.close();
                }
            }
        }
    }

    @Override
    public void sessionClosed(Session session) throws Throwable {
        log.info("session {} closed", session.getSessionId());
        try {
            if (session.getAttachment() instanceof AbstractHTTPConnection) {
                AbstractHTTPConnection httpConnection = (AbstractHTTPConnection) session.getAttachment();
                if (httpConnection != null) {
                    if (httpConnection.getClosedListener() != null) {
                        try {
                            httpConnection.getClosedListener().call(httpConnection);
                        } catch (Throwable t1) {
                            log.error("http connection closed listener error", t1);
                        }
                    }

                    if (httpConnection.isOpen()) {
                        httpConnection.close();
                    }
                }
            }
        } catch (Throwable t) {
            log.error("http2 conection close exception", t);
        }
    }

}
