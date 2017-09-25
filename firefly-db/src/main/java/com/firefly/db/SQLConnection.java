package com.firefly.db;

import com.firefly.utils.function.Func1;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface SQLConnection {

    /**
     * Query single column record by SQL
     *
     * @param sql    An SQL that may contain one or more '?' IN parameter placeholders
     * @param params SQL parameters
     * @param <T>    The type of column
     * @return The future result
     * @throws RecordNotFound If the database has not record throw RecordNotFound exception
     */
    <T> CompletableFuture<T> queryForSingleColumn(String sql, Object... params);

    /**
     * Query record and bind object
     *
     * @param sql    An SQL that may contain one or more '?' IN parameter placeholders
     * @param clazz  The Class reference of bound object
     * @param params SQL parameters
     * @param <T>    The type of bound object
     * @return The future result
     * @throws RecordNotFound If the database has not record throw RecordNotFound exception
     */
    <T> CompletableFuture<T> queryForObject(String sql, Class<T> clazz, Object... params);

    /**
     * Query record by id
     *
     * @param id    Primary key
     * @param clazz The Class reference of bound object
     * @param <T>   The type of bound object
     * @return The future result
     * @throws RecordNotFound If the database has not record of this id throw RecordNotFound exception
     */
    <T> CompletableFuture<T> queryById(Object id, Class<T> clazz);

    /**
     * Query records and convert records to a Map
     *
     * @param sql        An SQL that may contain one or more '?' IN parameter placeholders
     * @param valueClass The Class reference of bound object
     * @param params     SQL parameters
     * @param <K>        The type of primary key
     * @param <V>        The type of bound object
     * @return The future result that contains a map, the key is primary key of record, the value is bound object.
     */
    <K, V> CompletableFuture<Map<K, V>> queryForBeanMap(String sql, Class<V> valueClass, Object... params);

    /**
     * Query records and bind object
     *
     * @param sql    An SQL that may contain one or more '?' IN parameter placeholders
     * @param clazz  The Class reference of bound object
     * @param params SQL parameters
     * @param <T>    The type of bound object
     * @return The future result that contains a list
     */
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
