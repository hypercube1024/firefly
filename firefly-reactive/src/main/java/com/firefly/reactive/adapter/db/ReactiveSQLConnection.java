package com.firefly.reactive.adapter.db;

import com.firefly.db.SQLConnection;
import com.firefly.db.SQLResultSet;
import com.firefly.db.TransactionIsolation;
import com.firefly.utils.function.Func1;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * SQLConnection reactor adapter
 *
 * @see com.firefly.db.SQLConnection
 * @author Pengtao Qiu
 */
public interface ReactiveSQLConnection {

    <T> Mono<T> queryForSingleColumn(String sql, Object... params);

    <T> Mono<T> queryForObject(String sql, Class<T> clazz, Object... params);

    <T> Mono<T> queryById(Object id, Class<T> clazz);

    <K, V> Mono<Map<K, V>> queryForBeanMap(String sql, Class<V> valueClass, Object... params);

    <T> Mono<List<T>> queryForList(String sql, Class<T> clazz, Object... params);

    <T> Mono<T> query(String sql, Func1<SQLResultSet, T> handler, Object... params);

    Mono<Integer> update(String sql, Object... params);

    <T> Mono<Integer> updateObject(T object);

    <T> Mono<T> insert(String sql, Object... params);

    <T, R> Mono<R> insertObject(T object);

    <T, R> Mono<R> insertObjectBatch(List<T> list, Class<T> clazz, Func1<SQLResultSet, R> handler);

    <T, R> Mono<List<R>> insertObjectBatch(List<T> list, Class<T> clazz);

    <R> Mono<R> insertBatch(String sql, Object[][] params, Func1<SQLResultSet, R> handler);

    <T> Mono<Integer> deleteById(Object id, Class<T> clazz);

    Mono<int[]> executeBatch(String sql, Object[][] params);

    Mono<Boolean> setTransactionIsolation(TransactionIsolation transactionIsolation);

    Mono<Boolean> setAutoCommit(boolean autoCommit);

    boolean getAutoCommit();

    Mono<Boolean> rollback();

    Mono<Boolean> commit();

    Mono<Boolean> close();

    Mono<Boolean> commitAndClose();

    Mono<Boolean> rollbackAndClose();

    <T> Mono<T> inTransaction(Func1<ReactiveSQLConnection, Mono<T>> func1);

    Mono<Boolean> beginTransaction();

    Mono<Boolean> rollbackAndEndTransaction();

    Mono<Boolean> commitAndEndTransaction();

    SQLConnection getSQLConnection();

}
