package com.firefly.reactive.adapter.db;

import com.firefly.db.SQLClient;
import com.firefly.utils.function.Func1;
import reactor.core.publisher.Mono;

/**
 * SQLClient reactor adapter
 *
 * @author Pengtao Qiu
 */
public interface ReactiveSQLClient {

    Mono<ReactiveSQLConnection> getConnection();

    <T> Mono<T> newTransaction(Func1<ReactiveSQLConnection, Mono<T>> func1);

    SQLClient getSQLClient();
}
