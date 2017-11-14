package com.firefly.reactive.adapter.db;

import com.firefly.db.SQLConnection;
import com.firefly.db.SQLResultSet;
import com.firefly.db.TransactionIsolation;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Func1;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class ReactiveSQLConnectionAdapter implements ReactiveSQLConnection {

    private final SQLConnection sqlConnection;

    public ReactiveSQLConnectionAdapter(SQLConnection sqlConnection) {
        this.sqlConnection = sqlConnection;
    }

    @Override
    public <T> Mono<T> queryForSingleColumn(String sql, Object... params) {
        return Mono.fromCompletionStage(sqlConnection.queryForSingleColumn(sql, params));
    }

    @Override
    public <T> Mono<T> namedQueryForSingleColumn(String sql, Map<String, Object> paramMap) {
        return Mono.fromCompletionStage(sqlConnection.namedQueryForSingleColumn(sql, paramMap));
    }

    @Override
    public <T> Mono<T> namedQueryForSingleColumn(String sql, Object paramObject) {
        return Mono.fromCompletionStage(sqlConnection.namedQueryForSingleColumn(sql, paramObject));
    }

    @Override
    public <T> Mono<T> queryForObject(String sql, Class<T> clazz, Object... params) {
        return Mono.fromCompletionStage(sqlConnection.queryForObject(sql, clazz, params));
    }

    @Override
    public <T> Mono<T> namedQueryForObject(String sql, Class<T> clazz, Map<String, Object> paramMap) {
        return Mono.fromCompletionStage(sqlConnection.namedQueryForObject(sql, clazz, paramMap));
    }

    @Override
    public <T> Mono<T> namedQueryForObject(String sql, Class<T> clazz, Object paramObject) {
        return Mono.fromCompletionStage(sqlConnection.namedQueryForObject(sql, clazz, paramObject));
    }

    @Override
    public <T> Mono<T> queryById(Object id, Class<T> clazz) {
        return Mono.fromCompletionStage(sqlConnection.queryById(id, clazz));
    }

    @Override
    public <K, V> Mono<Map<K, V>> queryForBeanMap(String sql, Class<V> valueClass, Object... params) {
        return Mono.fromCompletionStage(sqlConnection.queryForBeanMap(sql, valueClass, params));
    }

    @Override
    public <K, V> Mono<Map<K, V>> namedQueryForBeanMap(String sql, Class<V> valueClass, Map<String, Object> paramMap) {
        return Mono.fromCompletionStage(sqlConnection.namedQueryForBeanMap(sql, valueClass, paramMap));
    }

    @Override
    public <K, V> Mono<Map<K, V>> namedQueryForBeanMap(String sql, Class<V> valueClass, Object paramObject) {
        return Mono.fromCompletionStage(sqlConnection.namedQueryForBeanMap(sql, valueClass, paramObject));
    }

    @Override
    public <T> Mono<List<T>> queryForList(String sql, Class<T> clazz, Object... params) {
        return Mono.fromCompletionStage(sqlConnection.queryForList(sql, clazz, params));
    }

    @Override
    public <T> Mono<List<T>> namedQueryForList(String sql, Class<T> clazz, Map<String, Object> paramMap) {
        return Mono.fromCompletionStage(sqlConnection.namedQueryForList(sql, clazz, paramMap));
    }

    @Override
    public <T> Mono<List<T>> namedQueryForList(String sql, Class<T> clazz, Object paramObject) {
        return Mono.fromCompletionStage(sqlConnection.namedQueryForList(sql, clazz, paramObject));
    }

    @Override
    public <T> Mono<T> query(String sql, Func1<SQLResultSet, T> handler, Object... params) {
        return Mono.fromCompletionStage(sqlConnection.query(sql, handler, params));
    }

    @Override
    public <T> Mono<T> namedQuery(String sql, Func1<SQLResultSet, T> handler, Map<String, Object> paramMap) {
        return Mono.fromCompletionStage(sqlConnection.namedQuery(sql, handler, paramMap));
    }

    @Override
    public <T> Mono<T> namedQuery(String sql, Func1<SQLResultSet, T> handler, Object paramObject) {
        return Mono.fromCompletionStage(sqlConnection.namedQuery(sql, handler, paramObject));
    }

    @Override
    public Mono<Integer> update(String sql, Object... params) {
        return Mono.fromCompletionStage(sqlConnection.update(sql, params));
    }

    @Override
    public Mono<Integer> namedUpdate(String sql, Map<String, Object> paramMap) {
        return Mono.fromCompletionStage(sqlConnection.namedUpdate(sql, paramMap));
    }

    @Override
    public Mono<Integer> namedUpdate(String sql, Object paramObject) {
        return Mono.fromCompletionStage(sqlConnection.namedUpdate(sql, paramObject));
    }

    @Override
    public <T> Mono<Integer> updateObject(T object) {
        return Mono.fromCompletionStage(sqlConnection.updateObject(object));
    }

    @Override
    public <T> Mono<T> insert(String sql, Object... params) {
        return Mono.fromCompletionStage(sqlConnection.insert(sql, params));
    }

    @Override
    public <T> Mono<T> namedInsert(String sql, Map<String, Object> paramMap) {
        return Mono.fromCompletionStage(sqlConnection.namedInsert(sql, paramMap));
    }

    @Override
    public <T> Mono<T> namedInsert(String sql, Object paramObject) {
        return Mono.fromCompletionStage(sqlConnection.namedInsert(sql, paramObject));
    }

    @Override
    public <T, R> Mono<R> insertObject(T object) {
        return Mono.fromCompletionStage(sqlConnection.insertObject(object));
    }

    @Override
    public <T, R> Mono<R> insertObjectBatch(List<T> list, Class<T> clazz, Func1<SQLResultSet, R> handler) {
        return Mono.fromCompletionStage(sqlConnection.insertObjectBatch(list, clazz, handler));
    }

    @Override
    public <T, R> Mono<List<R>> insertObjectBatch(List<T> list, Class<T> clazz) {
        return Mono.fromCompletionStage(sqlConnection.insertObjectBatch(list, clazz));
    }

    @Override
    public <R> Mono<R> insertBatch(String sql, Object[][] params, Func1<SQLResultSet, R> handler) {
        return Mono.fromCompletionStage(sqlConnection.insertBatch(sql, params, handler));
    }

    @Override
    public <T> Mono<Integer> deleteById(Object id, Class<T> clazz) {
        return Mono.fromCompletionStage(sqlConnection.deleteById(id, clazz));
    }

    @Override
    public Mono<int[]> executeBatch(String sql, Object[][] params) {
        return Mono.fromCompletionStage(sqlConnection.executeBatch(sql, params));
    }

    @Override
    public Mono<Boolean> setTransactionIsolation(TransactionIsolation transactionIsolation) {
        return Mono.create(sink -> sqlConnection.setTransactionIsolation(transactionIsolation).thenAccept(aVoid -> sink.success(true)).exceptionally(x -> {
            sink.error(x);
            return null;
        }));
    }

    @Override
    public Mono<Boolean> setAutoCommit(boolean autoCommit) {
        return Mono.create(sink -> sqlConnection.setAutoCommit(autoCommit).thenAccept(aVoid -> sink.success(true)).exceptionally(x -> {
            sink.error(x);
            return null;
        }));
    }

    @Override
    public boolean getAutoCommit() {
        return sqlConnection.getAutoCommit();
    }

    @Override
    public Mono<Boolean> rollback() {
        return Mono.create(sink -> sqlConnection.rollback().thenAccept(aVoid -> sink.success(true)).exceptionally(x -> {
            sink.error(x);
            return null;
        }));
    }

    @Override
    public Mono<Boolean> commit() {
        return Mono.create(sink -> sqlConnection.commit().thenAccept(aVoid -> sink.success(true)).exceptionally(x -> {
            sink.error(x);
            return null;
        }));
    }

    @Override
    public Mono<Boolean> close() {
        return Mono.create(sink -> sqlConnection.close().thenAccept(aVoid -> sink.success(true)).exceptionally(x -> {
            sink.error(x);
            return null;
        }));
    }

    @Override
    public Mono<Boolean> commitAndClose() {
        return Mono.create(sink -> sqlConnection.commitAndClose().thenAccept(aVoid -> sink.success(true)).exceptionally(x -> {
            sink.error(x);
            return null;
        }));
    }

    @Override
    public Mono<Boolean> rollbackAndClose() {
        return Mono.create(sink -> sqlConnection.rollbackAndClose().thenAccept(aVoid -> sink.success(true)).exceptionally(x -> {
            sink.error(x);
            return null;
        }));
    }

    @Override
    public <T> Mono<T> inTransaction(Func1<ReactiveSQLConnection, Mono<T>> func1) {
        return Mono.fromCompletionStage(sqlConnection.inTransaction(conn -> {
            Promise.Completable<T> completable = new Promise.Completable<>();
            func1.call(this)
                 .subscribe(completable::succeeded, completable::failed);
            return completable;
        }));
    }

    @Override
    public Mono<Boolean> beginTransaction() {
        return Mono.fromCompletionStage(sqlConnection.beginTransaction());
    }

    @Override
    public Mono<Boolean> rollbackAndEndTransaction() {
        return Mono.create(sink -> sqlConnection.rollbackAndEndTransaction().thenAccept(aVoid -> sink.success(true)).exceptionally(x -> {
            sink.error(x);
            return null;
        }));
    }

    @Override
    public Mono<Boolean> commitAndEndTransaction() {
        return Mono.create(sink -> sqlConnection.commitAndEndTransaction().thenAccept(aVoid -> sink.success(true)).exceptionally(x -> {
            sink.error(x);
            return null;
        }));
    }

    @Override
    public SQLConnection getSQLConnection() {
        return sqlConnection;
    }
}
