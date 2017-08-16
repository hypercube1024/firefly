package com.firefly.db;

import com.firefly.utils.function.Func1;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface SQLConnection {

    <T> CompletableFuture<T> queryForSingleColumn(String sql, Object... params);

    <T> CompletableFuture<T> queryForObject(String sql, Class<T> clazz, Object... params);

    <T> CompletableFuture<T> queryById(Object id, Class<T> clazz);

    <K, V> CompletableFuture<Map<K, V>> queryForBeanMap(String sql, Class<V> valueClass, Object... params);

    <T> CompletableFuture<List<T>> queryForList(String sql, Class<T> clazz, Object... params);

    <T> CompletableFuture<T> query(String sql, Func1<SQLResultSet, T> handler, Object... params);

    CompletableFuture<Integer> update(String sql, Object... params);

    <T> CompletableFuture<Integer> updateObject(T object);

    <T> CompletableFuture<T> insert(String sql, Object... params);

    <T, R> CompletableFuture<R> insertObject(T object);

    <T, R> CompletableFuture<R> insertObjectBatch(List<T> list, Class<T> clazz, Func1<SQLResultSet, R> handler);

    <T, R> CompletableFuture<List<R>> insertObjectBatch(List<T> list, Class<T> clazz);

    <R> CompletableFuture<R> insertBatch(String sql, Object[][] params, Func1<SQLResultSet, R> handler);

    <T> CompletableFuture<Integer> deleteById(Object id, Class<T> clazz);

    CompletableFuture<int[]> executeBatch(String sql, Object[][] params);

    CompletableFuture<Void> setTransactionIsolation(TransactionIsolation transactionIsolation);

    CompletableFuture<Void> setAutoCommit(boolean autoCommit);

    boolean getAutoCommit();

    CompletableFuture<Void> rollback();

    CompletableFuture<Void> commit();

    CompletableFuture<Void> close();

    CompletableFuture<Void> commitAndClose();

    CompletableFuture<Void> rollbackAndClose();

    <T> CompletableFuture<T> inTransaction(Func1<SQLConnection, CompletableFuture<T>> func1);

    CompletableFuture<Boolean> beginTransaction();

    CompletableFuture<Void> rollbackAndEndTransaction();

    CompletableFuture<Void> commitAndEndTransaction();

}
