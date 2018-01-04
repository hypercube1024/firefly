package com.firefly.server.http2;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.stream.AbstractHTTPHandler;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.SecureSessionFactory;
import com.firefly.net.Session;
import com.firefly.utils.StringUtils;

import java.util.Optional;

public class HTTP2ServerHandler extends AbstractHTTPHandler {

    private final ServerSessionListener listener;
    private final ServerHTTPHandler serverHTTPHandler;
    private final WebSocketHandler webSocketHandler;

    public HTTP2ServerHandler(HTTP2Configuration config,
                              ServerSessionListener listener,
                              ServerHTTPHandler serverHTTPHandler,
                              WebSocketHandler webSocketHandler) {
        super(config);
        this.listener = listener;
        this.serverHTTPHandler = serverHTTPHandler;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void sessionOpened(final Session session) throws Throwable {
        if (config.isSecureConnectionEnabled()) {
            SecureSessionFactory factory = config.getSecureSessionFactory();
            session.attachObject(factory.create(session, false, sslSession -> {
                log.debug("server session {} SSL handshake finished", session.getSessionId());
                HTTPConnection httpConnection;
                String protocol = Optional.ofNullable(sslSession.getApplicationProtocol())
                                          .filter(StringUtils::hasText)
                                          .orElse("http/1.1");
                switch (protocol) {
                    case "http/1.1":
                        httpConnection = new HTTP1ServerConnection(config, session, sslSession, new HTTP1ServerRequestHandler(serverHTTPHandler), listener, webSocketHandler);
                        break;
                    case "h2":
                        httpConnection = new HTTP2ServerConnection(config, session, sslSession, listener);
                        break;
                    default:
                        throw new IllegalStateException("SSL application protocol negotiates failure. The protocol " + protocol + " is not supported");
                }
                session.attachObject(httpConnection);
                serverHTTPHandler.acceptConnection(httpConnection);
            }));
        } else {
            if (!StringUtils.hasText(config.getProtocol())) {
                HTTPConnection httpConnection = new HTTP1ServerConnection(config, session, null, new HTTP1ServerRequestHandler(serverHTTPHandler), listener, webSocketHandler);
                session.attachObject(httpConnection);
                serverHTTPHandler.acceptConnection(httpConnection);
            } else {
                HttpVersion httpVersion = HttpVersion.fromString(config.getProtocol());
                if (httpVersion == null) {
                    throw new IllegalArgumentException("the protocol " + config.getProtocol() + " is not support.");
                }
                switch (httpVersion) {
                    case HTTP_1_1: {
                        HTTPConnection httpConnection = new HTTP1ServerConnection(config, session, null, new HTTP1ServerRequestHandler(serverHTTPHandler), listener, webSocketHandler);
                        session.attachObject(httpConnection);
                        serverHTTPHandler.acceptConnection(httpConnection);
                    }
                    break;
                    case HTTP_2: {
                        HTTPConnection httpConnection = new HTTP2ServerConnection(config, session, null, listener);
                        session.attachObject(httpConnection);
                        serverHTTPHandler.acceptConnection(httpConnection);
                    }
                    break;
                    default:
                        throw new IllegalArgumentException("the protocol " + config.getProtocol() + " is not support.");
                }
            }
        }
    }

}
