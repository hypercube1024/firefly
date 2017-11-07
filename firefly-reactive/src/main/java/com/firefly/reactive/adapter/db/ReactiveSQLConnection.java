package com.firefly.reactive.adapter.db;

import com.firefly.db.SQLConnection;
import com.firefly.db.SQLResultSet;
import com.firefly.db.TransactionIsolation;
import com.firefly.utils.function.Func1;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * SQLConnection reactor adapter. It wraps SQLConnection using Spring reactor.
 *
 * @author Pengtao Qiu
 */
public interface ReactiveSQLConnection {

    /**
     * Query single column record by SQL. If the database has not record, it will emit the RecordNotFound exception.
     *
     * @param sql    An SQL that may contain one or more '?' IN parameter placeholders
     * @param params SQL parameters
     * @param <T>    The type of column
     * @return The result that is wrapped by Mono
     */
    <T> Mono<T> queryForSingleColumn(String sql, Object... params);

    /**
     * Query record and bind object. If the database has not record, it will emit the RecordNotFound exception.
     *
     * @param sql    An SQL that may contain one or more '?' IN parameter placeholders
     * @param clazz  The Class reference of bound object
     * @param params SQL parameters
     * @param <T>    The type of bound object
     * @return The result that binds to Class and it is wrapped by Mono.
     */
    <T> Mono<T> queryForObject(String sql, Class<T> clazz, Object... params);

    /**
     * Query record by id. If the database has not record, it will emit the RecordNotFound exception. For example:
     * <blockquote>
     * <pre>
     * @Test
     * public void testRecordNotFound() {
     *     StepVerifier.create(exec(c -> c.queryById(size + 10, User.class)))
     *     .expectErrorMatches(t -> t.getCause() instanceof RecordNotFound)
     *     .verify();
     * }
     * </pre>
     * </blockquote>
     *
     * @param id    Primary key
     * @param clazz The Class reference of bound object
     * @param <T>   The type of bound object
     * @return The result that binds to Class and it is wrapped by Mono.
     */
    <T> Mono<T> queryById(Object id, Class<T> clazz);

    /**
     * Query records and convert records to a Map
     *
     * @param sql        An SQL that may contain one or more '?' IN parameter placeholders
     * @param valueClass The Class reference of bound object
     * @param params     SQL parameters
     * @param <K>        The type of primary key
     * @param <V>        The type of bound object
     * @return The result that contains a map, the key is primary key of record, the value is bound object. And it is wrapped by Mono.
     */
    <K, V> Mono<Map<K, V>> queryForBeanMap(String sql, Class<V> valueClass, Object... params);

    /**
     * Query records and bind object
     *
     * @param sql    An SQL that may contain one or more '?' IN parameter placeholders
     * @param clazz  The Class reference of bound object
     * @param params SQL parameters
     * @param <T>    The type of bound object
     * @return The result that contains a list, the list element binds to Class. And it is wrapped by Mono.
     */
    <T> Mono<List<T>> queryForList(String sql, Class<T> clazz, Object... params);

    <T> Mono<T> query(String sql, Func1<SQLResultSet, T> handler, Object... params);

    Mono<Integer> update(String sql, Object... params);

    /**
     * Update record using the mapped javabean.
     *
     * @param object The mapped javabean.
     * @param <T>    The type of javabean.
     * @return The affected row number.
     */
    <T> Mono<Integer> updateObject(T object);

    <T> Mono<T> insert(String sql, Object... params);

    /**
     * Insert a javabean to the database.
     *
     * @param object The javabean that is mapped to a database table.
     * @param <T>    The type of javabean.
     * @param <R>    The type of autoincrement id.
     * @return The autoincrement id that is wrapped by Mono.
     */
    <T, R> Mono<R> insertObject(T object);

    <T, R> Mono<R> insertObjectBatch(List<T> list, Class<T> clazz, Func1<SQLResultSet, R> handler);

    <T, R> Mono<List<R>> insertObjectBatch(List<T> list, Class<T> clazz);

    <R> Mono<R> insertBatch(String sql, Object[][] params, Func1<SQLResultSet, R> handler);

    /**
     * Delete data by id and return the affected row number.
     *
     * @param id    The table row id.
     * @param clazz The Mapped javabean Class.
     * @param <T>   The type of javabean.
     * @return Affected row number.
     */
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
