package com.firefly.server.http2.router.handler.error;

import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractErrorResponseHandler implements Handler {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    @Override
    public void handle(RoutingContext ctx) {
        if (ctx.hasNext()) {
            try {
                ctx.next();
            } catch (Throwable t) {
                log.error("http handler exception", t);
                if (!ctx.getResponse().isCommited()) {
                    render(ctx, HttpStatus.INTERNAL_SERVER_ERROR_500, t);
                }
            }
        } else {
            render(ctx, HttpStatus.NOT_FOUND_404, null);
        }
    }

    abstract public void render(RoutingContext ctx, int status, Throwable t);
}
