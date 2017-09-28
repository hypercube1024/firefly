package com.firefly.example.reactive.coffee.store.router.impl.sys;

import com.firefly.$;
import com.firefly.annotation.Component;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.example.reactive.coffee.store.vo.Response;
import com.firefly.example.reactive.coffee.store.vo.ResponseStatus;
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
                if (ex instanceof IllegalArgumentException) {
                    renderIllegalArgumentException(ctx, (IllegalArgumentException) ex);
                } else if (ex.getCause() instanceof IllegalArgumentException) {
                    renderIllegalArgumentException(ctx, (IllegalArgumentException) ex.getCause());
                } else if (ex instanceof IllegalStateException) {
                    renderIllegalStateException(ctx, (IllegalStateException) ex);
                } else if (ex.getCause() instanceof IllegalStateException) {
                    renderIllegalStateException(ctx, (IllegalStateException) ex.getCause());
                } else {
                    ctx.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
                    logger.error(() -> "Server exception. " + ctx.getURI(), ex);
                    Response<Boolean> response = new Response<>();
                    response.setStatus(ResponseStatus.SERVER_ERROR.getValue());
                    response.setMessage("server exception, " + ex.getMessage());
                    ctx.writeJson(response).end();
                }
            });
    }

    private void renderIllegalArgumentException(RoutingContext ctx, IllegalArgumentException ex) {
        ctx.setStatus(HttpStatus.OK_200);
        logger.info(() -> $.string.replace("Request error. [{}], [{}]", ctx.getURI(), ex.getMessage()));
        Response<Boolean> response = new Response<>();
        response.setStatus(ResponseStatus.ARGUMENT_ERROR.getValue());
        response.setMessage(ex.getMessage());
        ctx.writeJson(response).end();
    }

    private void renderIllegalStateException(RoutingContext ctx, IllegalStateException ex) {
        ctx.setStatus(HttpStatus.OK_200);
        logger.info(() -> $.string.replace("Request error. [{}], [{}]", ctx.getURI(), ex.getMessage()));
        Response<Boolean> response = new Response<>();
        response.setStatus(ResponseStatus.SERVER_ERROR.getValue());
        response.setMessage(ex.getMessage());
        ctx.writeJson(response).end();
    }
}
