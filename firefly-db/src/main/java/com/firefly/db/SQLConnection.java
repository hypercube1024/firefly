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
     *                 "select * from test where id = &id".
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
     *                    "select * from test where id = &id".
     * @param paramObject Named SQL parameter object that uses the property name to match parameter.
     * @param <T>         The type of column.
     * @return The future result.
     */
    <T> CompletableFuture<T> namedQueryForSingleColumn(String sql, Object paramObject);

    /**
     * Query record and bind object. If the database has not record, it will emit the RecordNotFound exception.
     *
     * @param sql    A SQL that may contain one or more '?' placeholders.
     * @param clazz  The Class reference of bound object.
     * @param params SQL parameters.
     * @param <T>    The type of bound object.
     * @return The future result.
     */
    <T> CompletableFuture<T> queryForObject(String sql, Class<T> clazz, Object... params);

    /**
     * Query record and bind object. If the database has not record, it will emit the RecordNotFound exception.
     *
     * @param sql      A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&", such as,
     *                 "select * from test where id in (:idList)",
     *                 "select * from test where id = :id",
     *                 "select * from test where id = :{id}",
     *                 "select * from test where id = &id".
     * @param clazz    The Class reference of bound object.
     * @param paramMap Named SQL parameters.
     * @param <T>      The type of bound object.
     * @return The future result.
     */
    <T> CompletableFuture<T> namedQueryForObject(String sql, Class<T> clazz, Map<String, Object> paramMap);

    /**
     * Query record and bind object. If the database has not record, it will emit the RecordNotFound exception.
     *
     * @param sql         A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&", such as,
     *                    "select * from test where id in (:idList)",
     *                    "select * from test where id = :id",
     *                    "select * from test where id = :{id}",
     *                    "select * from test where id = &id".
     * @param clazz       The Class reference of bound object.
     * @param paramObject Named SQL parameter object that uses the property name to match parameter.
     * @param <T>         The type of bound object.
     * @return The future result.
     */
    <T> CompletableFuture<T> namedQueryForObject(String sql, Class<T> clazz, Object paramObject);

    /**
     * Query record by id. If the database has not record, it will emit the RecordNotFound exception.
     *
     * @param id    Primary key.
     * @param clazz The Class reference of bound object.
     * @param <T>   The type of bound object.
     * @return The future result.
     */
    <T> CompletableFuture<T> queryById(Object id, Class<T> clazz);

    /**
     * Query records and convert records to a Map.
     *
     * @param sql        A SQL that may contain one or more '?' placeholders.
     * @param valueClass The Class reference of bound object.
     * @param params     SQL parameters.
     * @param <K>        The type of primary key.
     * @param <V>        The type of bound object.
     * @return The future result that contains a map, the key is primary key of record, the value is bound object.
     */
    <K, V> CompletableFuture<Map<K, V>> queryForBeanMap(String sql, Class<V> valueClass, Object... params);

    /**
     * Query records and convert records to a Map
     *
     * @param sql        A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&", such as,
     *                   "select * from test where id in (:idList)",
     *                   "select * from test where id = :id",
     *                   "select * from test where id = :{id}",
     *                   "select * from test where id = &id".
     * @param valueClass The Class reference of bound object.
     * @param paramMap   Named SQL parameters.
     * @param <K>        The type of primary key.
     * @param <V>        The type of bound object.
     * @return The future result that contains a map, the key is primary key of record, the value is bound object.
     */
    <K, V> CompletableFuture<Map<K, V>> namedQueryForBeanMap(String sql, Class<V> valueClass, Map<String, Object> paramMap);

    /**
     * Query records and convert records to a Map
     *
     * @param sql         A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&", such as,
     *                    "select * from test where id in (:idList)",
     *                    "select * from test where id = :id",
     *                    "select * from test where id = :{id}",
     *                    "select * from test where id = &id"
     * @param valueClass  The Class reference of bound object.
     * @param paramObject Named SQL parameters that uses the property name to match parameter.
     * @param <K>         The type of primary key.
     * @param <V>         The type of bound object.
     * @return The future result that contains a map, the key is primary key of record, the value is bound object.
     */
    <K, V> CompletableFuture<Map<K, V>> namedQueryForBeanMap(String sql, Class<V> valueClass, Object paramObject);

    /**
     * Query records and bind object.
     *
     * @param sql    A SQL that may contain one or more '?' placeholders.
     * @param clazz  The Class reference of bound object.
     * @param params SQL parameters.
     * @param <T>    The type of bound object.
     * @return The future result that contains a list.
     */
    <T> CompletableFuture<List<T>> queryForList(String sql, Class<T> clazz, Object... params);

    /**
     * Query records and bind object.
     *
     * @param sql      A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&", such as,
     *                 "select * from test where id in (:idList)",
     *                 "select * from test where id = :id",
     *                 "select * from test where id = :{id}",
     *                 "select * from test where id = &id"
     * @param clazz    The Class reference of bound object.
     * @param paramMap Named SQL parameters.
     * @param <T>      The type of bound object.
     * @return The future result that contains a list.
     */
    <T> CompletableFuture<List<T>> namedQueryForList(String sql, Class<T> clazz, Map<String, Object> paramMap);

    /**
     * Query records and bind object.
     *
     * @param sql         A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&", such as,
     *                    "select * from test where id in (:idList)",
     *                    "select * from test where id = :id",
     *                    "select * from test where id = :{id}",
     *                    "select * from test where id = &id"
     * @param clazz       The Class reference of bound object.
     * @param paramObject Named SQL parameters.
     * @param <T>         The type of bound object.
     * @return The future result that contains a list.
     */
    <T> CompletableFuture<List<T>> namedQueryForList(String sql, Class<T> clazz, Object paramObject);

    /**
     * Query records and convert result set to javabean using handler.
     *
     * @param sql     A SQL that may contain one or more '?' placeholders.
     * @param handler The function that converts result set to javabean.
     * @param params  SQL parameters.
     * @param <T>     The type of converted object.
     * @return The result that is wrapped by CompletableFuture.
     */
    <T> CompletableFuture<T> query(String sql, Func1<SQLResultSet, T> handler, Object... params);

    /**
     * Query records and convert result set to javabean using handler.
     *
     * @param sql      A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&", such as,
     *                 "select * from test where id in (:idList)",
     *                 "select * from test where id = :id",
     *                 "select * from test where id = :{id}",
     *                 "select * from test where id = &id"
     * @param handler  The function that converts result set to javabean.
     * @param paramMap Named SQL parameters.
     * @param <T>      The type of converted object.
     * @return The result that is wrapped by CompletableFuture.
     */
    <T> CompletableFuture<T> namedQuery(String sql, Func1<SQLResultSet, T> handler, Map<String, Object> paramMap);

    /**
     * Query records and convert result set to javabean using handler.
     *
     * @param sql         A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&", such as,
     *                    "select * from test where id in (:idList)",
     *                    "select * from test where id = :id",
     *                    "select * from test where id = :{id}",
     *                    "select * from test where id = &id"
     * @param handler     The function that converts result set to javabean.
     * @param paramObject Named SQL parameters.
     * @param <T>         The type of converted object.
     * @return The result that is wrapped by CompletableFuture.
     */
    <T> CompletableFuture<T> namedQuery(String sql, Func1<SQLResultSet, T> handler, Object paramObject);

    /**
     * Update records.
     *
     * @param sql    A SQL that may contain one or more '?' placeholders.
     * @param params SQL parameters.
     * @return The affected row number.
     */
    CompletableFuture<Integer> update(String sql, Object... params);

    /**
     * Update records.
     *
     * @param sql      A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&".
     * @param paramMap Named SQL parameters.
     * @return The affected row number.
     */
    CompletableFuture<Integer> namedUpdate(String sql, Map<String, Object> paramMap);

    /**
     * Update records.
     *
     * @param sql         A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&".
     * @param paramObject Named SQL parameters.
     * @return The affected row number.
     */
    CompletableFuture<Integer> namedUpdate(String sql, Object paramObject);

    /**
     * Update record using the mapped javabean.
     *
     * @param object The mapped javabean.
     * @param <T>    The type of javabean.
     * @return The affected row number.
     */
    <T> CompletableFuture<Integer> updateObject(T object);

    /**
     * Insert records
     *
     * @param sql    A SQL that may contain one or more '?' placeholders.
     * @param params SQL parameters.
     * @param <T>    The type of autoincrement id.
     * @return The autoincrement id that is wrapped by CompletableFuture.
     */
    <T> CompletableFuture<T> insert(String sql, Object... params);

    /**
     * Insert records
     *
     * @param sql      A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&".
     * @param paramMap Named SQL parameters.
     * @param <T>      The type of autoincrement id.
     * @return The autoincrement id that is wrapped by CompletableFuture.
     */
    <T> CompletableFuture<T> namedInsert(String sql, Map<String, Object> paramMap);

    /**
     * Insert records
     *
     * @param sql         A SQL that may contain one or more placeholders. The placeholder starts with ":" or "&".
     * @param paramObject Named SQL parameters.
     * @param <T>         The type of autoincrement id.
     * @return The autoincrement id that is wrapped by CompletableFuture.
     */
    <T> CompletableFuture<T> namedInsert(String sql, Object paramObject);

    /**
     * Insert a javabean to the database.
     *
     * @param object The javabean that is mapped to a database table.
     * @param <T>    The type of javabean.
     * @param <R>    The type of autoincrement id.
     * @return The autoincrement id that is wrapped by CompletableFuture.
     */
    <T, R> CompletableFuture<R> insertObject(T object);

    /**
     * Batch to insert javabean.
     *
     * @param list    The javabean list.
     * @param clazz   The javabean Class.
     * @param handler The function that converts result set to java type.
     * @param <T>     The type of javabean.
     * @param <R>     The type of autoincrement id.
     * @return The autoincrement id list that is wrapped by CompletableFuture.
     */
    <T, R> CompletableFuture<R> insertObjectBatch(List<T> list, Class<T> clazz, Func1<SQLResultSet, R> handler);

    /**
     * Batch to insert javabean.
     *
     * @param list  The javabean list.
     * @param clazz The javabean Class.
     * @param <T>   The type of javabean.
     * @param <R>   The type of autoincrement id.
     * @return The autoincrement id list that is wrapped by CompletableFuture.
     */
    <T, R> CompletableFuture<List<R>> insertObjectBatch(List<T> list, Class<T> clazz);

    /**
     * Execute a sql to batch inserting data.
     *
     * @param sql     A SQL that may contain one or more '?' placeholders.
     * @param params  An array of query replacement parameters.  Each row in
     *                this array is one set of batch replacement values.
     * @param handler The function that converts result set to java type.
     * @param <R>     The type of autoincrement id.
     * @return The autoincrement id list that is wrapped by CompletableFuture.
     */
    <R> CompletableFuture<R> insertBatch(String sql, Object[][] params, Func1<SQLResultSet, R> handler);

    /**
     * Delete data by id and return the affected row number.
     *
     * @param id    The table row id.
     * @param clazz The Mapped javabean Class.
     * @param <T>   The type of javabean.
     * @return Affected row number.
     */
    <T> CompletableFuture<Integer> deleteById(Object id, Class<T> clazz);

    /**
     * Execute a batch of SQL INSERT, UPDATE, or DELETE queries.
     *
     * @param sql    A SQL that may contain one or more '?' placeholders.
     * @param params An array of query replacement parameters.  Each row in
     *               this array is one set of batch replacement values.
     * @return The number of rows updated per statement.
     */
    CompletableFuture<int[]> executeBatch(String sql, Object[][] params);

    /**
     * Set the transaction isolation.
     *
     * @param transactionIsolation The transaction isolation.
     * @return If return true, set the transaction isolation success.
     */
    CompletableFuture<Void> setTransactionIsolation(TransactionIsolation transactionIsolation);

    /**
     * Set transaction committing automatically.
     *
     * @param autoCommit If set the true, the transaction will commit automatically.
     * @return If return true, Set auto committing success.
     */
    CompletableFuture<Void> setAutoCommit(boolean autoCommit);

    /**
     * Get auto committing.
     *
     * @return If return true, the transaction will commit automatically.
     */
    boolean getAutoCommit();

    /**
     * Rollback the transaction.
     *
     * @return If return true, the transaction rollback success.
     */
    CompletableFuture<Void> rollback();

    /**
     * Commit a transaction.
     *
     * @return If return true, the transaction committing success.
     */
    CompletableFuture<Void> commit();

    /**
     * Close the connection.
     *
     * @return If return true, close connection success.
     */
    CompletableFuture<Void> close();

    /**
     * Commit and then close connection.
     *
     * @return If return true, commit and then close connection success.
     */
    CompletableFuture<Void> commitAndClose();

    /**
     * Rollback and then close connection.
     *
     * @return If return true, rollback and then close connection success.
     */
    CompletableFuture<Void> rollbackAndClose();

    /**
     * Execute the statements in transaction.
     *
     * @param func1 The statements will be executed in a transaction.
     * @param <T>   The type of transaction result.
     * @return The transaction result that is wrapped by Mono.
     */
    <T> CompletableFuture<T> inTransaction(Func1<SQLConnection, CompletableFuture<T>> func1);

    /**
     * Begin a transaction.
     *
     * @return If return true, begin transaction success.
     */
    CompletableFuture<Boolean> beginTransaction();

    /**
     * Rollback and then end the transaction.
     *
     * @return If return true, rollback and then end the transaction success.
     */
    CompletableFuture<Void> rollbackAndEndTransaction();

    /**
     * Commit and then end the transaction.
     *
     * @return If return true, commit and then end the transaction success.
     */
    CompletableFuture<Void> commitAndEndTransaction();
}
