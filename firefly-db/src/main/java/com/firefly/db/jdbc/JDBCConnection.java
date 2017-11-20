package com.firefly.db.jdbc;

import com.firefly.db.*;
import com.firefly.db.jdbc.helper.JDBCHelper;
import com.firefly.db.jdbc.helper.namedparam.ParsedSql;
import com.firefly.db.jdbc.helper.namedparam.PreparedSqlAndValues;
import com.firefly.utils.BeanUtils;
import com.firefly.utils.collection.ConcurrentLinkedHashMap;
import com.firefly.utils.concurrent.Promise.Completable;
import com.firefly.utils.function.Func1;
import com.firefly.utils.lang.bean.PropertyAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.firefly.db.jdbc.helper.namedparam.NamedParameterParser.parseSqlStatement;
import static com.firefly.db.jdbc.helper.namedparam.NamedParameterParser.replaceParsedSql;

/**
 * @author Pengtao Qiu
 */
public class JDBCConnection implements SQLConnection {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    protected static final Map<String, ParsedSql> namedParamCache = new ConcurrentLinkedHashMap<>(true, 256);

    private final JDBCHelper jdbcHelper;
    private final Connection connection;
    private final AtomicBoolean autoCommit;
    private final AtomicBoolean inTransaction;

    public JDBCConnection(JDBCHelper jdbcHelper, Connection connection) {
        this.jdbcHelper = jdbcHelper;
        this.connection = connection;
        try {
            autoCommit = new AtomicBoolean(connection.getAutoCommit());
        } catch (SQLException e) {
            throw new DBException(e);
        }
        inTransaction = new AtomicBoolean(false);
    }

    public JDBCHelper getJdbcHelper() {
        return jdbcHelper;
    }

    public Connection getConnection() {
        return connection;
    }

    protected static ParsedSql parseSql(String sql) {
        ParsedSql parsedSql = namedParamCache.get(sql);
        if (parsedSql == null) {
            parsedSql = parseSqlStatement(sql);
            namedParamCache.put(sql, parsedSql);
        }
        return parsedSql;
    }

    protected static PreparedSqlAndValues getPreparedSqlAndValues(String sql, Map<String, Object> paramMap) {
        return replaceParsedSql(parseSql(sql), paramMap);
    }

    protected static PreparedSqlAndValues getPreparedSqlAndValues(String sql, Object object) {
        Map<String, PropertyAccess> beanAccess = BeanUtils.getBeanAccess(object.getClass());
        Map<String, Object> paramMap = new HashMap<>();
        beanAccess.forEach((name, property) -> paramMap.put(name, property.getValue(object)));
        return getPreparedSqlAndValues(sql, paramMap);
    }

    @Override
    public <T> CompletableFuture<T> queryForSingleColumn(String sql, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForSingleColumn(connection, sql, params));
    }

    @Override
    public <T> CompletableFuture<T> namedQueryForSingleColumn(String sql, Map<String, Object> paramMap) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramMap);
        return queryForSingleColumn(preparedSqlAndValues.getPreparedSql(), preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public <T> CompletableFuture<T> namedQueryForSingleColumn(String sql, Object paramObject) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramObject);
        return queryForSingleColumn(preparedSqlAndValues.getPreparedSql(), preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public <T> CompletableFuture<T> queryForObject(String sql, Class<T> clazz, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForObject(connection, sql, clazz, params));
    }

    @Override
    public <T> CompletableFuture<T> namedQueryForObject(String sql, Class<T> clazz, Map<String, Object> paramMap) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramMap);
        return queryForObject(preparedSqlAndValues.getPreparedSql(), clazz, preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public <T> CompletableFuture<T> namedQueryForObject(String sql, Class<T> clazz, Object paramObject) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramObject);
        return queryForObject(preparedSqlAndValues.getPreparedSql(), clazz, preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public <T> CompletableFuture<T> queryById(Object id, Class<T> clazz) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryById(connection, clazz, id));
    }

    @Override
    public <K, V> CompletableFuture<Map<K, V>> queryForBeanMap(String sql, Class<V> valueClass, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForBeanMap(connection, sql, valueClass, params));
    }

    @Override
    public <K, V> CompletableFuture<Map<K, V>> namedQueryForBeanMap(String sql, Class<V> valueClass, Map<String, Object> paramMap) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramMap);
        return queryForBeanMap(preparedSqlAndValues.getPreparedSql(), valueClass, preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public <K, V> CompletableFuture<Map<K, V>> namedQueryForBeanMap(String sql, Class<V> valueClass, Object paramObject) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramObject);
        return queryForBeanMap(preparedSqlAndValues.getPreparedSql(), valueClass, preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public <T> CompletableFuture<List<T>> queryForList(String sql, Class<T> clazz, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForList(connection, sql, clazz, params));
    }

    @Override
    public <T> CompletableFuture<List<T>> namedQueryForList(String sql, Class<T> clazz, Map<String, Object> paramMap) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramMap);
        return queryForList(preparedSqlAndValues.getPreparedSql(), clazz, preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public <T> CompletableFuture<List<T>> namedQueryForList(String sql, Class<T> clazz, Object paramObject) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramObject);
        return queryForList(preparedSqlAndValues.getPreparedSql(), clazz, preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public <T> CompletableFuture<T> query(String sql, Func1<SQLResultSet, T> handler, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                return Optional.ofNullable(helper.getRunner().query(connection, sql, rs -> handler.call(new JDBCResultSet(rs)), params))
                               .orElseThrow(RecordNotFound::new);
            } catch (SQLException e) {
                throw new DBException(e);
            }
        });
    }

    @Override
    public <T> CompletableFuture<T> namedQuery(String sql, Func1<SQLResultSet, T> handler, Map<String, Object> paramMap) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramMap);
        return query(preparedSqlAndValues.getPreparedSql(), handler, preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public <T> CompletableFuture<T> namedQuery(String sql, Func1<SQLResultSet, T> handler, Object paramObject) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramObject);
        return query(preparedSqlAndValues.getPreparedSql(), handler, preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public CompletableFuture<Integer> update(String sql, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.update(connection, sql, params));
    }

    @Override
    public CompletableFuture<Integer> namedUpdate(String sql, Map<String, Object> paramMap) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramMap);
        return update(preparedSqlAndValues.getPreparedSql(), preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public CompletableFuture<Integer> namedUpdate(String sql, Object paramObject) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramObject);
        return update(preparedSqlAndValues.getPreparedSql(), preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public <T> CompletableFuture<Integer> updateObject(T object) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.updateObject(connection, object));
    }

    @Override
    public <T> CompletableFuture<T> insert(String sql, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.insert(connection, sql, params));
    }

    @Override
    public <T> CompletableFuture<T> namedInsert(String sql, Map<String, Object> paramMap) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramMap);
        return insert(preparedSqlAndValues.getPreparedSql(), preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public <T> CompletableFuture<T> namedInsert(String sql, Object paramObject) {
        PreparedSqlAndValues preparedSqlAndValues = getPreparedSqlAndValues(sql, paramObject);
        return insert(preparedSqlAndValues.getPreparedSql(), preparedSqlAndValues.getValues().toArray());
    }

    @Override
    public <T, R> CompletableFuture<R> insertObject(T object) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.insertObject(connection, object));
    }

    @Override
    public <T, R> CompletableFuture<R> insertObjectBatch(List<T> list, Class<T> clazz, Func1<SQLResultSet, R> handler) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.insertObjectBatch(connection, rs -> handler.call(new JDBCResultSet(rs)), clazz, list));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, R> CompletableFuture<List<R>> insertObjectBatch(List<T> list, Class<T> clazz) {
        return insertObjectBatch(list, clazz, rs -> {
            List<R> ret = new ArrayList<>();
            while (rs.next()) {
                ret.add((R) rs.getObject(1));
            }
            return ret;
        });
    }

    @Override
    public <R> CompletableFuture<R> insertBatch(String sql, Object[][] params, Func1<SQLResultSet, R> handler) {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                return helper.getRunner().insertBatch(connection, sql, rs -> handler.call(new JDBCResultSet(rs)), params);
            } catch (SQLException e) {
                throw new DBException(e);
            }
        });
    }

    @Override
    public <T> CompletableFuture<Integer> deleteById(Object id, Class<T> clazz) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.deleteById(connection, clazz, id));
    }

    @Override
    public CompletableFuture<int[]> executeBatch(String sql, Object[][] params) {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                return helper.getRunner().batch(connection, sql, params);
            } catch (SQLException e) {
                throw new DBException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> setTransactionIsolation(TransactionIsolation transactionIsolation) {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                connection.setTransactionIsolation(toTransactionIsolationLevel(transactionIsolation));
            } catch (SQLException e) {
                throw new DBException(e);
            }
            return null;
        });
    }

    private int toTransactionIsolationLevel(TransactionIsolation transactionIsolation) {
        switch (transactionIsolation) {
            case NONE:
                return Connection.TRANSACTION_NONE;
            case READ_UNCOMMITTED:
                return Connection.TRANSACTION_READ_UNCOMMITTED;
            case READ_COMMITTED:
                return Connection.TRANSACTION_READ_COMMITTED;
            case REPEATABLE_READ:
                return Connection.TRANSACTION_REPEATABLE_READ;
            case SERIALIZABLE:
                return Connection.TRANSACTION_SERIALIZABLE;
            default:
                throw new DBException("transaction isolation error");
        }
    }

    @Override
    public CompletableFuture<Void> setAutoCommit(boolean autoCommit) {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                connection.setAutoCommit(autoCommit);
                this.autoCommit.set(autoCommit);
                log.debug("jdbc connection auto commit -> {}", autoCommit);
            } catch (SQLException e) {
                throw new DBException(e);
            }
            return null;
        });
    }

    @Override
    public boolean getAutoCommit() {
        return autoCommit.get();
    }

    @Override
    public CompletableFuture<Void> rollback() {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                connection.rollback();
            } catch (SQLException e) {
                throw new DBException(e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> commit() {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                connection.commit();
            } catch (SQLException e) {
                throw new DBException(e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> close() {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new DBException(e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> commitAndClose() {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try (Connection c = connection) {
                c.commit();
                log.debug("jdbc connection commit and close");
            } catch (SQLException e) {
                throw new DBException(e);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> rollbackAndClose() {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try (Connection c = connection) {
                c.rollback();
                log.debug("jdbc connection rollback and close");
            } catch (SQLException e) {
                throw new DBException(e);
            }
            return null;
        });
    }

    @Override
    public <T> CompletableFuture<T> inTransaction(Func1<SQLConnection, CompletableFuture<T>> func1) {
        Completable<T> ret = new Completable<>();
        beginTransaction().thenAccept(newTransaction -> executeFuncAndCommit(func1, ret, newTransaction));
        return ret;
    }

    @Override
    public CompletableFuture<Boolean> beginTransaction() {
        Completable<Boolean> ret = new Completable<>();
        if (inTransaction.compareAndSet(false, true)) {
            if (getAutoCommit()) {
                setAutoCommit(false).thenAccept(v -> ret.succeeded(true))
                                    .exceptionally(e -> {
                                        inTransaction.set(false);
                                        ret.failed(e);
                                        return null;
                                    });
                log.debug("jdbc connection start new transaction and set auto commit is false");
            } else {
                log.debug("jdbc connection start new transaction");
                ret.succeeded(true);
            }
        } else {
            log.debug("jdbc connection in transaction");
            ret.succeeded(false);
        }
        return ret;
    }

    @Override
    public CompletableFuture<Void> rollbackAndEndTransaction() {
        Completable<Void> ret = new Completable<>();
        rollbackAndClose().thenAccept(c -> {
            inTransaction.set(false);
            ret.succeeded(null);
        }).exceptionally(e -> {
            inTransaction.set(false);
            ret.failed(e);
            return null;
        });
        return ret;
    }

    @Override
    public CompletableFuture<Void> commitAndEndTransaction() {
        Completable<Void> ret = new Completable<>();
        commitAndClose().thenAccept(c -> {
            inTransaction.set(false);
            ret.succeeded(null);
        }).exceptionally(e -> {
            inTransaction.set(false);
            ret.failed(e);
            return null;
        });
        return ret;
    }

    private <T> void executeFuncAndCommit(Func1<SQLConnection, CompletableFuture<T>> func1, Completable<T> ret, boolean commit) {
        if (commit) {
            try {
                func1.call(this)
                     .thenAccept(r -> commitAndEndTransaction().thenAccept(v -> ret.succeeded(r)).exceptionally(t -> {
                         log.error("jdbc connection commit and end transaction exception", t);
                         ret.failed(t);
                         return null;
                     }))
                     .exceptionally(t -> {
                         log.error("jdbc connection executes transaction exception", t);
                         rollbackAndEndTransaction().thenAccept(c -> ret.failed(t)).exceptionally(t1 -> {
                             log.error("jdbc connection rollback and end transaction exception", t1);
                             ret.failed(t1);
                             return null;
                         });
                         return null;
                     });
            } catch (Exception e) {
                log.error("jdbc connection end transaction exception", e);
                rollbackAndEndTransaction().thenAccept(c -> ret.failed(e)).exceptionally(t -> {
                    log.error("jdbc connection rollback and end transaction exception", t);
                    ret.failed(t);
                    return null;
                });
            }
        } else {
            try {
                func1.call(this)
                     .thenAccept(ret::succeeded)
                     .exceptionally(t -> {
                         rollback().thenAccept(c -> ret.failed(t)).exceptionally(t1 -> {
                             log.error("jdbc connection rollback exception", t1);
                             ret.failed(t1);
                             return null;
                         });
                         return null;
                     });
            } catch (Exception e) {
                log.error("jdbc connection exception", e);
                rollback().thenAccept(c -> ret.failed(e)).exceptionally(t -> {
                    log.error("jdbc connection rollback exception", e);
                    ret.failed(t);
                    return null;
                });
            }
        }
    }
}
