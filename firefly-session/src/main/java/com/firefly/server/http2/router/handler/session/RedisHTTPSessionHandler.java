package com.firefly.server.http2.router.handler.session;

import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.SessionStore;
import com.firefly.server.http2.router.impl.RoutingContextImpl;
import com.firefly.server.http2.router.spi.HTTPSessionHandlerSPI;
import com.firefly.utils.lang.AbstractLifeCycle;

/**
 * @author Pengtao Qiu
 */
public class RedisHTTPSessionHandler extends AbstractLifeCycle implements Handler {

    private final SessionStore sessionStore;
    private final HTTPSessionConfiguration configuration;

    public RedisHTTPSessionHandler(RedisSessionStore sessionStore) {
        this(new HTTPSessionConfiguration(), sessionStore);
    }

    public RedisHTTPSessionHandler(HTTPSessionConfiguration configuration, RedisSessionStore sessionStore) {
        this.sessionStore = sessionStore;
        this.configuration = configuration;
        start();
    }

    @Override
    public void handle(RoutingContext context) {
        RoutingContextImpl ctx = (RoutingContextImpl) context;
        HTTPSessionHandlerSPI sessionHandlerSPI = new HTTPSessionHandlerSPIImpl(ctx, sessionStore, configuration);
        ctx.setHTTPSessionHandlerSPI(sessionHandlerSPI);
        ctx.next();
    }

    @Override
    protected void init() {
        sessionStore.start();
    }

    @Override
    protected void destroy() {
        sessionStore.stop();
    }
}
