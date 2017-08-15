package com.firefly.db;

import com.firefly.utils.function.Func1;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface SQLClient {

    CompletableFuture<SQLConnection> getConnection();

    <T> CompletableFuture<T> inTransaction(Func1<SQLConnection, CompletableFuture<T>> func1);

}
