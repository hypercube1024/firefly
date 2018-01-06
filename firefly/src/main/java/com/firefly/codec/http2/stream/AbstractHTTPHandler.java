package com.firefly.codec.http2.stream;

import com.firefly.codec.websocket.stream.impl.WebSocketConnectionImpl;
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
    public void messageReceived(Session session, Object message) {
    }

    @Override
    public void exceptionCaught(Session session, Throwable t) throws Throwable {
        try {
            log.error("HTTP handler exception", t);
            Object attachment = session.getAttachment();
            if (attachment == null) {
                return;
            }
            if (attachment instanceof AbstractHTTPConnection) {
                try (AbstractHTTPConnection httpConnection = (AbstractHTTPConnection) attachment) {
                    httpConnection.notifyException(t);
                } catch (Exception e) {
                    log.error("The http connection exception listener error", e);
                }
            } else if (attachment instanceof WebSocketConnectionImpl) {
                try (WebSocketConnectionImpl webSocketConnection = (WebSocketConnectionImpl) attachment) {
                    webSocketConnection.notifyException(t);
                } catch (Exception e) {
                    log.error("The websocket connection exception listener error", e);
                }
            }
        } finally {
            session.close();
        }
    }

    @Override
    public void sessionClosed(Session session) throws Throwable {
        log.info("The HTTP handler received the session {} closed event.", session.getSessionId());
        Object attachment = session.getAttachment();
        if (attachment == null) {
            return;
        }
        if (attachment instanceof AbstractHTTPConnection) {
            try (AbstractHTTPConnection httpConnection = (AbstractHTTPConnection) attachment) {
                httpConnection.notifyClose();
            } catch (Exception e) {
                log.error("The http2 connection close exception", e);
            }
        } else if (attachment instanceof WebSocketConnectionImpl) {
            try (WebSocketConnectionImpl webSocketConnection = (WebSocketConnectionImpl) attachment) {
                webSocketConnection.notifyClose();
            } catch (Exception e) {
                log.error("The websocket connection close exception", e);
            }
        }
    }

}
