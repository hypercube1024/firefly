package com.firefly.client.http2;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.AbstractHTTPHandler;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;
import com.firefly.utils.StringUtils;

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
                String protocol = sslSession.applicationProtocol();
                if (StringUtils.hasText(protocol)) {
                    switch (protocol) {
                        case "http/1.1":
                            initializeHTTP1ClientConnection(session, context, sslSession);
                            break;
                        case "h2":
                            initializeHTTP2ClientConnection(session, context, sslSession);
                            break;
                        default:
                            throw new IllegalStateException("SSL application protocol negotiates failure. The protocol " + protocol + " is not supported");
                    }
                } else {
                    throw new IllegalStateException("SSL application protocol negotiates exception, the protocol is null");
                }
            }));
        } else {
            if (!StringUtils.hasText(config.getProtocol())) {
                initializeHTTP1ClientConnection(session, context, null);
            } else {
                HttpVersion httpVersion = HttpVersion.fromString(config.getProtocol());
                if (httpVersion == null) {
                    throw new IllegalArgumentException("the protocol " + config.getProtocol() + " is not support.");
                }
                switch (httpVersion) {
                    case HTTP_1_1:
                        initializeHTTP1ClientConnection(session, context, null);
                        break;
                    case HTTP_2:
                        initializeHTTP2ClientConnection(session, context, null);
                        break;
                    default:
                        throw new IllegalArgumentException("the protocol " + config.getProtocol() + " is not support.");
                }
            }

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
