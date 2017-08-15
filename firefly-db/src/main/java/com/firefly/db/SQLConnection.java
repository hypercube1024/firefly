package com.firefly.db;

import com.firefly.utils.concurrent.Promise.Completable;
import com.firefly.utils.function.Func1;

import java.util.List;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public interface SQLConnection {

    <T> Completable<T> queryForSingleColumn(String sql, Object... params);

    <T> Completable<T> queryForObject(String sql, Class<T> clazz, Object... params);

    <T> Completable<T> queryById(Object id, Class<T> clazz);

    <K, V> Completable<Map<K, V>> queryForBeanMap(String sql, Class<V> valueClass, Object... params);

    <T> Completable<List<T>> queryForList(String sql, Class<T> clazz, Object... params);

    <T> Completable<T> query(String sql, Func1<SQLResultSet, T> handler, Object... params);

    Completable<Integer> update(String sql, Object... params);

    <T> Completable<Integer> updateObject(T object);

    <T> Completable<T> insert(String sql, Object... params);

    <T, R> Completable<R> insertObject(T object);

    <T, R> Completable<R> insertObjectBatch(List<T> list, Class<T> clazz, Func1<SQLResultSet, R> handler);

    <T, R> Completable<List<R>> insertObjectBatch(List<T> list, Class<T> clazz);

    <R> Completable<R> insertBatch(String sql, Object[][] params, Func1<SQLResultSet, R> handler);

    <T> Completable<Integer> deleteById(Object id, Class<T> clazz);

    Completable<int[]> executeBatch(String sql, Object[][] params);

    Completable<Void> setTransactionIsolation(TransactionIsolation transactionIsolation);

    Completable<Void> setAutoCommit(boolean autoCommit);

    boolean getAutoCommit();

    Completable<Void> rollback();

    Completable<Void> commit();

    Completable<Void> close();

    Completable<Void> commitAndClose();

    Completable<Void> rollbackAndClose();

    <T> Completable<T> inTransaction(Func1<SQLConnection, Completable<T>> func1);

}
