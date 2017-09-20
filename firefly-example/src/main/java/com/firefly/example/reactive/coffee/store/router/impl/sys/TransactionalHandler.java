package com.firefly.example.reactive.coffee.store.router.impl.sys;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.reactive.adapter.db.ReactiveTransactionalManager;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.log.slf4j.ext.LazyLogger;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
@Component("transactionalHandler")
public class TransactionalHandler implements Handler {

    private static LazyLogger logger = LazyLogger.create();

    @Inject
    private ReactiveTransactionalManager db;

    private final HttpMethod[] methods = {HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE};

    @Override
    public void handle(RoutingContext ctx) {
        db.beginTransaction()
          .then(begin -> {
              logger.info(() -> "transaction begin -> " + ctx.getURI().getPathQuery());
              return Mono.fromFuture(ctx.nextFuture());
          })
          .then(process -> {
              logger.info(() -> "transaction end -> " + ctx.getURI().getPathQuery());
              return db.commitAndEndTransaction();
          })
          .subscribe(success -> {
              logger.info(() -> "transaction submit success -> " + ctx.getURI().getPathQuery());
              ctx.succeed(true);
          }, ex -> db.rollbackAndEndTransaction().subscribe(rollbackSuccess -> ctx.fail(ex), ctx::fail));
    }

    public HttpMethod[] getMethods() {
        return methods;
    }
}
