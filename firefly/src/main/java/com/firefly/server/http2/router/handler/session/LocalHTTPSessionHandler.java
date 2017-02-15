package com.firefly.server.http2.router.handler.session;

import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Pengtao Qiu
 */
public class LocalHTTPSessionHandler implements Handler {

    private final ConcurrentMap<String, HTTPSessionImpl> sessionMap;
    private final LocalHTTPSessionConfiguration configuration;

    public LocalHTTPSessionHandler(LocalHTTPSessionConfiguration configuration) {
        this.configuration = configuration;
        this.sessionMap = new ConcurrentHashMap<>();
    }

    @Override
    public void handle(RoutingContext ctx) {
        HTTPSessionHandlerSPIImpl sessionHandlerSPI = new HTTPSessionHandlerSPIImpl(sessionMap, ctx, configuration);
        ctx.setHTTPSessionHandlerSPI(sessionHandlerSPI);
    }

}
