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
public class LocalHTTPSessionHandler extends AbstractLifeCycle implements Handler {

    private final SessionStore sessionStore = DefaultSessionStoreLoader.getInstance().getSessionStore();
    private final HTTPSessionConfiguration configuration;

    public LocalHTTPSessionHandler() {
        this(new HTTPSessionConfiguration());
    }

    public LocalHTTPSessionHandler(HTTPSessionConfiguration configuration) {
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

    public SessionStore getSessionStore() {
        return sessionStore;
    }

    public HTTPSessionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    protected void init() {

    }

    @Override
    protected void destroy() {
        sessionStore.stop();
    }
}
