package com.firefly.db.jdbc;

import com.firefly.db.DBException;
import com.firefly.db.SQLConnection;
import com.firefly.db.SQLResultSet;
import com.firefly.db.TransactionIsolation;
import com.firefly.db.jdbc.helper.JDBCHelper;
import com.firefly.utils.concurrent.Promise.Completable;
import com.firefly.utils.function.Func1;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Pengtao Qiu
 */
public class JDBCConnection implements SQLConnection {

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

    @Override
    public <T> CompletableFuture<T> queryForSingleColumn(String sql, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForSingleColumn(connection, sql, params));
    }

    @Override
    public <T> CompletableFuture<T> queryForObject(String sql, Class<T> clazz, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForObject(connection, sql, clazz, params));
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
    public <T> CompletableFuture<List<T>> queryForList(String sql, Class<T> clazz, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForList(connection, sql, clazz, params));
    }

    @Override
    public <T> CompletableFuture<T> query(String sql, Func1<SQLResultSet, T> handler, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                return helper.getRunner().query(connection, sql, rs -> handler.call(new JDBCResultSet(rs)), params);
            } catch (SQLException e) {
                throw new DBException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Integer> update(String sql, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.update(connection, sql, params));
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
            } catch (SQLException e) {
                throw new DBException(e);
            }
            return null;
        });
    }

    @Override
    public <T> CompletableFuture<T> inTransaction(Func1<SQLConnection, CompletableFuture<T>> func1) {
        if (inTransaction.compareAndSet(false, true)) {
            Completable<T> ret = new Completable<>();
            if (getAutoCommit()) {
                setAutoCommit(false).thenAccept(c -> executeFunc(func1, ret))
                                    .exceptionally(e -> {
                                        inTransaction.set(false);
                                        ret.failed(e);
                                        return null;
                                    });
            } else {
                executeFunc(func1, ret);
            }
            return ret;
        } else {
            return func1.call(this);
        }
    }

    private <T> void executeFunc(Func1<SQLConnection, CompletableFuture<T>> func1, Completable<T> ret) {
        try {
            func1.call(this)
                 .thenAccept(c -> commitAndEndTransaction(ret, c))
                 .exceptionally(e -> {
                     rollbackAndEndTransaction(ret, e);
                     return null;
                 });
        } catch (Exception e) {
            rollbackAndEndTransaction(ret, e);
        }
    }

    private <T> void commitAndEndTransaction(Completable<T> ret, T c) {
        commitAndClose().thenAccept(c1 -> {
            inTransaction.set(false);
            ret.succeeded(c);
        }).exceptionally(e -> {
            inTransaction.set(false);
            ret.failed(e);
            return null;
        });
    }

    private <T> void rollbackAndEndTransaction(Completable<T> ret, Throwable e) {
        rollbackAndClose().thenAccept(c -> {
            inTransaction.set(false);
            ret.failed(e);
        }).exceptionally(t -> {
            inTransaction.set(false);
            ret.failed(e);
            return null;
        });
    }
}
