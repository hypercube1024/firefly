package com.firefly.server.http2;

import com.firefly.codec.http2.stream.AbstractHTTPHandler;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.SSLSession;

public class HTTP2ServerHandler extends AbstractHTTPHandler {

    private final ServerSessionListener listener;
    private final ServerHTTPHandler serverHTTPHandler;

    public HTTP2ServerHandler(HTTP2Configuration config, ServerSessionListener listener, ServerHTTPHandler serverHTTPHandler) {
        super(config);
        this.listener = listener;
        this.serverHTTPHandler = serverHTTPHandler;
    }

    @Override
    public void sessionOpened(final Session session) throws Throwable {
        if (config.isSecureConnectionEnabled()) {
            session.attachObject(new SSLSession(config.getSslContextFactory(), false, session, sslSession -> {
                log.debug("server session {} SSL handshake finished", session.getSessionId());
                HTTPConnection httpConnection;
                if ("http/1.1".equals(sslSession.applicationProtocol())) {
                    httpConnection = new HTTP1ServerConnection(config, session, sslSession, new HTTP1ServerRequestHandler(serverHTTPHandler), listener);
                } else {
                    httpConnection = new HTTP2ServerConnection(config, session, sslSession, listener);
                }
                session.attachObject(httpConnection);
                serverHTTPHandler.acceptConnection(httpConnection);
            }));
        } else {
            HTTPConnection httpConnection = new HTTP1ServerConnection(config, session, null,
                    new HTTP1ServerRequestHandler(serverHTTPHandler), listener);
            session.attachObject(httpConnection);
            serverHTTPHandler.acceptConnection(httpConnection);
        }
    }

}
