package com.firefly.example.reactive.coffee.store.router.impl.sys;

import com.firefly.$;
import com.firefly.annotation.Component;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.example.reactive.coffee.store.vo.Response;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.log.slf4j.ext.LazyLogger;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * @author Pengtao Qiu
 */
@Component("globalHandler")
public class GlobalHandler implements Handler {

    private static final LazyLogger logger = LazyLogger.create();

    @Override
    public void handle(RoutingContext ctx) {
        ctx.getResponse().setAsynchronous(true);
        Mono.fromFuture(ctx.nextFuture())
            .timeout(Duration.ofSeconds(1))
            .subscribe(ret -> ctx.end(), ex -> {
                IllegalArgumentException e = toIllegalArgumentException(ex);
                if (e != null) {
                    renderIllegalArgumentException(ctx, e);
                } else {
                    ctx.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    logger.error(() -> "Server exception. " + ctx.getURI(), ex);
                    Response<Boolean> response = new Response<>();
                    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    response.setMessage("server exception, " + ex.getMessage());
                    ctx.writeJson(response).end();
                }
            });
    }

    private IllegalArgumentException toIllegalArgumentException(Throwable ex) {
        if (ex instanceof IllegalArgumentException) {
            return (IllegalArgumentException) ex;
        } else if (ex.getCause() instanceof IllegalArgumentException) {
            return (IllegalArgumentException) ex.getCause();
        } else {
            return null;
        }
    }

    private void renderIllegalArgumentException(RoutingContext ctx, IllegalArgumentException ex) {
        ctx.setStatus(HttpStatus.BAD_REQUEST_400);
        logger.info(() -> $.string.replace("Request error. [{}], [{}]", ctx.getURI(), ex.getMessage()));
        Response<Boolean> response = new Response<>();
        response.setStatus(HttpStatus.BAD_REQUEST_400);
        response.setMessage(ex.getMessage());
        ctx.writeJson(response).end();
    }
}
