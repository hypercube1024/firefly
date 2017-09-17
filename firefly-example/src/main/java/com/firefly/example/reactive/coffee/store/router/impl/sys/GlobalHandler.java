package com.firefly.example.reactive.coffee.store.router.impl.sys;

import com.firefly.annotation.Component;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.example.reactive.coffee.store.vo.Response;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.log.slf4j.ext.LazyLogger;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
@Component("globalHandler")
public class GlobalHandler implements Handler {

    private static final LazyLogger logger = LazyLogger.create();

    @Override
    public void handle(RoutingContext ctx) {
        ctx.getResponse().setAsynchronous(true);
        Mono.fromFuture(ctx.nextFuture()).subscribe(ret -> ctx.end(), ex -> {
            ctx.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            logger.error(() -> "server exception", ex);
            Response<Boolean> response = new Response<>();
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            response.setMessage("server exception, " + ex.getMessage());
            ctx.writeJson(response).end();
        });
    }
}
