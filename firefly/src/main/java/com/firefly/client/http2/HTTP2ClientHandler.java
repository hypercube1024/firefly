package com.firefly.client.http2;

import com.firefly.codec.http2.stream.AbstractHTTPHandler;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;

import java.util.Map;

public class HTTP2ClientHandler extends AbstractHTTPHandler {

    private final Map<Integer, HTTP2ClientContext> http2ClientContext;

    public HTTP2ClientHandler(HTTP2Configuration config, Map<Integer, HTTP2ClientContext> http2ClientContext) {
        super(config);
        this.http2ClientContext = http2ClientContext;
    }

    @Override
    public void sessionOpened(final Session session) throws Throwable {
        final HTTP2ClientContext context = http2ClientContext.get(session.getSessionId());

        if (context == null) {
            log.error("http2 client can not get the client context of session {}", session.getSessionId());
            session.closeNow();
            return;
        }

        if (config.isSecureConnectionEnabled()) {
            session.attachObject(new SSLSession(config.getSslContextFactory(), true, session, sslSession -> {
                log.debug("client session {} SSL handshake finished", session.getSessionId());
                if ("http/1.1".equals(sslSession.applicationProtocol())) {
                    initializeHTTP1ClientConnection(session, context, sslSession);
                } else {
                    initializeHTTP2ClientConnection(session, context, sslSession);
                }
            }));
        } else {
            initializeHTTP1ClientConnection(session, context, null);
        }
    }

    private void initializeHTTP1ClientConnection(final Session session, final HTTP2ClientContext context,
                                                 final SSLSession sslSession) {
        try {
            HTTP1ClientConnection http1ClientConnection = new HTTP1ClientConnection(config, session, sslSession);
            session.attachObject(http1ClientConnection);
            context.promise.succeeded(http1ClientConnection);
        } catch (Throwable t) {
            context.promise.failed(t);
        } finally {
            http2ClientContext.remove(session.getSessionId());
        }
    }

    private void initializeHTTP2ClientConnection(final Session session, final HTTP2ClientContext context,
                                                 final SSLSession sslSession) {
        try {
            final HTTP2ClientConnection connection = new HTTP2ClientConnection(config, session, sslSession,
                    context.listener);
            session.attachObject(connection);
            connection.initialize(config, context.promise, context.listener);
        } finally {
            http2ClientContext.remove(session.getSessionId());
        }
    }

    @Override
    public void sessionClosed(Session session) throws Throwable {
        try {
            super.sessionClosed(session);
        } finally {
            http2ClientContext.remove(session.getSessionId());
        }
    }

    @Override
    public void failedOpeningSession(Integer sessionId, Throwable t) throws Throwable {
        try {
            HTTP2ClientContext context = http2ClientContext.get(sessionId);
            if (context != null) {
                context.promise.failed(t);
            }
        } finally {
            http2ClientContext.remove(sessionId);
        }
    }

    @Override
    public void exceptionCaught(Session session, Throwable t) throws Throwable {
        try {
            super.exceptionCaught(session, t);
        } finally {
            http2ClientContext.remove(session.getSessionId());
        }
    }

}
