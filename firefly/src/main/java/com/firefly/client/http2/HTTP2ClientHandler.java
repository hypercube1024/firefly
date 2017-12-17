package com.firefly.client.http2;

import com.firefly.codec.http2.frame.PingFrame;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.AbstractHTTPHandler;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.net.SecureSession;
import com.firefly.net.SecureSessionFactory;
import com.firefly.net.Session;
import com.firefly.utils.StringUtils;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class HTTP2ClientHandler extends AbstractHTTPHandler {

    private final Map<Integer, HTTP2ClientContext> http2ClientContext;
    private final Scheduler pingScheduler;
    private final Map<Integer, Scheduler.Future> pingSchedulerFutureMap;

    public HTTP2ClientHandler(HTTP2Configuration config, Map<Integer, HTTP2ClientContext> http2ClientContext) {
        super(config);
        this.http2ClientContext = http2ClientContext;
        pingScheduler = Schedulers.createScheduler();
        pingSchedulerFutureMap = new ConcurrentHashMap<>();
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
            SecureSessionFactory factory = config.getSecureSessionFactory();
            session.attachObject(factory.create(session, true, sslSession -> {
                String protocol = Optional.ofNullable(sslSession.getApplicationProtocol())
                                          .filter(StringUtils::hasText)
                                          .orElse("http/1.1");
                log.info("Client session {} SSL handshake finished. The app protocol is {}", session.getSessionId(), protocol);
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
                                                 final SecureSession sslSession) {
        try {
            HTTP1ClientConnection http1ClientConnection = new HTTP1ClientConnection(config, session, sslSession);
            session.attachObject(http1ClientConnection);
            context.getPromise().succeeded(http1ClientConnection);
        } catch (Throwable t) {
            context.getPromise().failed(t);
        } finally {
            http2ClientContext.remove(session.getSessionId());
        }
    }

    private void initializeHTTP2ClientConnection(final Session session, final HTTP2ClientContext context,
                                                 final SecureSession sslSession) {
        try {
            final HTTP2ClientConnection connection = new HTTP2ClientConnection(config, session, sslSession, context.getListener());
            session.attachObject(connection);
            connection.initialize(config, context.getPromise(), context.getListener());
            int id = session.getSessionId();

            Scheduler.Future future = pingScheduler.scheduleAtFixedRate(
                    () -> connection.getHttp2Session().ping(new PingFrame(false), new Callback() {
                        public void succeeded() {
                            log.info("the session {} sends ping frame success", id);
                        }

                        public void failed(Throwable x) {
                            log.warn("the session {} sends ping frame failure. {}", id, x.getMessage());
                        }
                    }),
                    config.getHttp2PingInterval(), config.getHttp2PingInterval(), TimeUnit.MILLISECONDS);
            pingSchedulerFutureMap.putIfAbsent(id, future);
        } finally {
            http2ClientContext.remove(session.getSessionId());
        }
    }

    @Override
    public void sessionClosed(Session session) throws Throwable {
        try {
            super.sessionClosed(session);
            Optional.ofNullable(pingSchedulerFutureMap.remove(session.getSessionId()))
                    .ifPresent(Scheduler.Future::cancel);
        } finally {
            http2ClientContext.remove(session.getSessionId());
        }
    }

    @Override
    public void failedOpeningSession(Integer sessionId, Throwable t) {
        Optional.ofNullable(http2ClientContext.remove(sessionId))
                .map(HTTP2ClientContext::getPromise)
                .ifPresent(promise -> promise.failed(t));
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
