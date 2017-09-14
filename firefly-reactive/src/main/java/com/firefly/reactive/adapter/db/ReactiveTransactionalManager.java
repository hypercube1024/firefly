package com.firefly.reactive.adapter.db;

import com.firefly.utils.function.Func1;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
public interface ReactiveTransactionalManager {

    Mono<ReactiveSQLConnection> getConnection();

    <T> Mono<T> execSQL(Func1<ReactiveSQLConnection, Mono<T>> func1);

    Mono<Boolean> beginTransaction();

    Mono<Boolean> rollbackAndEndTransaction();

    Mono<Boolean> commitAndEndTransaction();
}
