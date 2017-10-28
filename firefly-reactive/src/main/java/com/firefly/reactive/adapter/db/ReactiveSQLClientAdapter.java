package com.firefly.reactive.adapter.db;

import com.firefly.db.SQLClient;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Func1;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
public class ReactiveSQLClientAdapter implements ReactiveSQLClient {

    private final SQLClient sqlClient;

    public ReactiveSQLClientAdapter(SQLClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public Mono<ReactiveSQLConnection> getConnection() {
        return Mono.fromCompletionStage(sqlClient.getConnection().thenApply(ReactiveSQLConnectionAdapter::new));
    }

    @Override
    public <T> Mono<T> newTransaction(Func1<ReactiveSQLConnection, Mono<T>> func1) {
        return Mono.fromCompletionStage(sqlClient.newTransaction(conn -> {
            Promise.Completable<T> completable = new Promise.Completable<>();
            func1.call(new ReactiveSQLConnectionAdapter(conn))
                 .subscribe(completable::succeeded, completable::failed);
            return completable;
        }));
    }

    @Override
    public SQLClient getSQLClient() {
        return sqlClient;
    }
}
