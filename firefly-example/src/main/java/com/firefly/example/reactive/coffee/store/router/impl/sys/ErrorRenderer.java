package com.firefly.example.reactive.coffee.store.router.impl.sys;

import com.firefly.annotation.Component;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.handler.error.AbstractErrorResponseHandler;
import com.firefly.server.http2.router.handler.error.DefaultErrorResponseHandlerLoader;

/**
 * @author Pengtao Qiu
 */
@Component
public class ErrorRenderer {

    private AbstractErrorResponseHandler errorHandler = DefaultErrorResponseHandlerLoader.getInstance().getHandler();

    public void renderError(RoutingContext ctx, int status) {
        errorHandler.render(ctx, status, null);
        ctx.succeed(true);
    }
}
