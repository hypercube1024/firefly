package com.firefly.codec.http2.stream;

import com.firefly.net.Handler;
import com.firefly.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public abstract class AbstractHTTPHandler implements Handler {

    protected static Logger log = LoggerFactory.getLogger("firefly-system");

    protected final HTTP2Configuration config;

    public AbstractHTTPHandler(HTTP2Configuration config) {
        this.config = config;
    }

    @Override
    public void messageReceived(Session session, Object message) {
    }

    @Override
    public void exceptionCaught(Session session, Throwable t) throws Throwable {
        try {
            log.error("HTTP handler exception", t);
            if (session.getAttachment() != null && session.getAttachment() instanceof AbstractHTTPConnection) {
                try (AbstractHTTPConnection httpConnection = (AbstractHTTPConnection) session.getAttachment()) {
                    Optional.ofNullable(httpConnection.getExceptionListener()).ifPresent(c -> {
                        c.call(httpConnection, t);
                        log.info("The HTTP handler called connection {} exception listener.", session.getSessionId());
                    });
                } catch (Exception e) {
                    log.error("http connection exception listener error", e);
                }
            }
        } finally {
            session.close();
        }
    }

    @Override
    public void sessionClosed(Session session) throws Throwable {
        log.info("The HTTP handler received the session {} closed event.", session.getSessionId());
        if (session.getAttachment() != null && session.getAttachment() instanceof AbstractHTTPConnection) {
            try (AbstractHTTPConnection httpConnection = (AbstractHTTPConnection) session.getAttachment()) {
                Optional.ofNullable(httpConnection.getClosedListener()).ifPresent(c -> {
                    c.call(httpConnection);
                    log.info("The HTTP handler called {} closed listener. Session: {}", httpConnection.getClass(), session.getSessionId());
                });
            } catch (Exception e) {
                log.error("http2 connection close exception", e);
            }
        }
    }

}
