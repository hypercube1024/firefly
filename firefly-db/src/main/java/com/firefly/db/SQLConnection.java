package com.firefly.db;

import com.firefly.utils.function.Func1;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The asynchronous SQL connection. It can execute SQL and bind result to Java bean.
 *
 * @author Pengtao Qiu
 */
public interface SQLConnection {

    /**
     * Query single column record by SQL. If the database has not record, it will emit the RecordNotFound exception.
     *
     * @param sql    A SQL that may contain one or more '?' placeholders.
     * @param params SQL parameters.
     * @param <T>    The type of column.
     * @return The future result.
     */
    <T> CompletableFuture<T> queryForSingleColumn(String sql, Object... params);

    /**
     * Query single column record by named SQL. If the database has not record, it will emit the RecordNotFound exception.
     *
     * @param sql      A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&", such as,
     *                 "select * from test where id in (:idList)",
     *                 "select * from test where id = :id",
     *                 "select * from test where id = :{id}",
     *                 "select * from test where id = &id"
     * @param paramMap Named SQL parameters.
     * @param <T>      The type of column.
     * @return The future result.
     */
    <T> CompletableFuture<T> namedQueryForSingleColumn(String sql, Map<String, Object> paramMap);

    /**
     * Query single column record by named SQL. If the database has not record, it will emit the RecordNotFound exception.
     *
     * @param sql         A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&", such as,
     *                    "select * from test where id in (:idList)",
     *                    "select * from test where id = :id",
     *                    "select * from test where id = :{id}",
     *                    "select * from test where id = &id"
     * @param paramObject Named SQL parameter object that uses the property name to match parameter.
     * @param <T>         The type of column.
     * @return The future result.
     */
    <T> CompletableFuture<T> namedQueryForSingleColumn(String sql, Object paramObject);

    /**
     * Query record and bind object. If the database has not record, it will emit the RecordNotFound exception.
     *
     * @param sql    A SQL that may contain one or more '?' placeholders
     * @param clazz  The Class reference of bound object
     * @param params SQL parameters
     * @param <T>    The type of bound object
     * @return The future result
     */
    <T> CompletableFuture<T> queryForObject(String sql, Class<T> clazz, Object... params);

    /**
     * Query record and bind object. If the database has not record, it will emit the RecordNotFound exception.
     *
     * @param sql      A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&", such as,
     *                 "select * from test where id in (:idList)",
     *                 "select * from test where id = :id",
     *                 "select * from test where id = :{id}",
     *                 "select * from test where id = &id"
     * @param clazz    The Class reference of bound object
     * @param paramMap Named SQL parameters.
     * @param <T>      The type of bound object
     * @return The future result
     */
    <T> CompletableFuture<T> namedQueryForObject(String sql, Class<T> clazz, Map<String, Object> paramMap);

    /**
     * Query record and bind object. If the database has not record, it will emit the RecordNotFound exception.
     *
     * @param sql         A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&", such as,
     *                    "select * from test where id in (:idList)",
     *                    "select * from test where id = :id",
     *                    "select * from test where id = :{id}",
     *                    "select * from test where id = &id"
     * @param clazz       The Class reference of bound object
     * @param paramObject Named SQL parameter object that uses the property name to match parameter.
     * @param <T>         The type of bound object
     * @return The future result
     */
    <T> CompletableFuture<T> namedQueryForObject(String sql, Class<T> clazz, Object paramObject);

    /**
     * Query record by id. If the database has not record, it will emit the RecordNotFound exception.
     *
     * @param id    Primary key
     * @param clazz The Class reference of bound object
     * @param <T>   The type of bound object
     * @return The future result
     */
    <T> CompletableFuture<T> queryById(Object id, Class<T> clazz);

    /**
     * Query records and convert records to a Map
     *
     * @param sql        A SQL that may contain one or more '?' placeholders
     * @param valueClass The Class reference of bound object
     * @param params     SQL parameters
     * @param <K>        The type of primary key
     * @param <V>        The type of bound object
     * @return The future result that contains a map, the key is primary key of record, the value is bound object.
     */
    <K, V> CompletableFuture<Map<K, V>> queryForBeanMap(String sql, Class<V> valueClass, Object... params);

    <K, V> CompletableFuture<Map<K, V>> namedQueryForBeanMap(String sql, Class<V> valueClass, Map<String, Object> paramMap);

    <K, V> CompletableFuture<Map<K, V>> namedQueryForBeanMap(String sql, Class<V> valueClass, Object paramObject);

    /**
     * Query records and bind object
     *
     * @param sql    An SQL that may contain one or more '?' placeholders
     * @param clazz  The Class reference of bound object
     * @param params SQL parameters
     * @param <T>    The type of bound object
     * @return The future result that contains a list
     */
    <T> CompletableFuture<List<T>> queryForList(String sql, Class<T> clazz, Object... params);

    <T> CompletableFuture<List<T>> namedQueryForList(String sql, Class<T> clazz, Map<String, Object> paramMap);

    <T> CompletableFuture<List<T>> namedQueryForList(String sql, Class<T> clazz, Object paramObject);

    <T> CompletableFuture<T> query(String sql, Func1<SQLResultSet, T> handler, Object... params);

    <T> CompletableFuture<T> namedQuery(String sql, Func1<SQLResultSet, T> handler, Map<String, Object> paramMap);

    <T> CompletableFuture<T> namedQuery(String sql, Func1<SQLResultSet, T> handler, Object paramObject);

    CompletableFuture<Integer> update(String sql, Object... params);

    CompletableFuture<Integer> namedUpdate(String sql, Map<String, Object> paramMap);

    CompletableFuture<Integer> namedUpdate(String sql, Object paramObject);

    <T> CompletableFuture<Integer> updateObject(T object);

    <T> CompletableFuture<T> insert(String sql, Object... params);

    <T> CompletableFuture<T> namedInsert(String sql, Map<String, Object> paramMap);

    <T> CompletableFuture<T> namedInsert(String sql, Object paramObject);

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
