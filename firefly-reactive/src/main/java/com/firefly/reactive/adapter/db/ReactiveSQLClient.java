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

    /**
     * Get a database connection.
     *
     * @return A database connection that is wrapped by Mono.
     */
    Mono<ReactiveSQLConnection> getConnection();

    /**
     * Execute a transaction.
     *
     * @param func1 To execute the transaction function.
     * @param <T>   The type of transaction result.
     * @return The transaction result that is wrapped by Mono.
     */
    <T> Mono<T> newTransaction(Func1<ReactiveSQLConnection, Mono<T>> func1);

    /**
     * Get original SQL client.
     *
     * @return The original SQL client.
     */
    SQLClient getSQLClient();
}
