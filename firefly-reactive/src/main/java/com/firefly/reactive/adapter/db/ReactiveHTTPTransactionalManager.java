package com.firefly.reactive.adapter.db;

import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.function.Func1;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
public class ReactiveHTTPTransactionalManager implements ReactiveTransactionalManager {

    private final String transactionKey = "_currentReactiveHTTPTransaction";
    private final ReactiveSQLClient reactiveSQLClient;

    public ReactiveHTTPTransactionalManager(ReactiveSQLClient reactiveSQLClient) {
        this.reactiveSQLClient = reactiveSQLClient;
    }

    public String getTransactionKey() {
        return transactionKey;
    }

    public ReactiveSQLClient getReactiveSQLClient() {
        return reactiveSQLClient;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Mono<ReactiveSQLConnection> getConnection() {
        return HTTP2ServerBuilder
                .getCurrentCtx().map(RoutingContext::getAttributes)
                .map(m -> (Mono<ReactiveSQLConnection>) m.computeIfAbsent(transactionKey, k -> reactiveSQLClient.getConnection()))
                .orElseGet(reactiveSQLClient::getConnection);
    }

    @Override
    public <T> Mono<T> execSQL(Func1<ReactiveSQLConnection, Mono<T>> func1) {
        return getConnection().then(c -> c.execSQL(func1));
    }

    @Override
    public Mono<Boolean> beginTransaction() {
        return getConnection().then(ReactiveSQLConnection::beginTransaction);
    }

    @Override
    public Mono<Boolean> rollbackAndEndTransaction() {
        return getConnection().then(ReactiveSQLConnection::rollbackAndEndTransaction);
    }

    @Override
    public Mono<Boolean> commitAndEndTransaction() {
        return getConnection().then(ReactiveSQLConnection::commitAndEndTransaction);
    }
}
