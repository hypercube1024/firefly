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
    public <T> Completable<T> queryForSingleColumn(String sql, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForSingleColumn(connection, sql, params));
    }

    @Override
    public <T> Completable<T> queryForObject(String sql, Class<T> clazz, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForObject(connection, sql, clazz, params));
    }

    @Override
    public <T> Completable<T> queryById(Object id, Class<T> clazz) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryById(connection, clazz, id));
    }

    @Override
    public <K, V> Completable<Map<K, V>> queryForBeanMap(String sql, Class<V> valueClass, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForBeanMap(connection, sql, valueClass, params));
    }

    @Override
    public <T> Completable<List<T>> queryForList(String sql, Class<T> clazz, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForList(connection, sql, clazz, params));
    }

    @Override
    public <T> Completable<T> query(String sql, Func1<SQLResultSet, T> handler, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                return helper.getRunner().query(connection, sql, rs -> handler.call(new JDBCResultSet(rs)), params);
            } catch (SQLException e) {
                throw new DBException(e);
            }
        });
    }

    @Override
    public Completable<Integer> update(String sql, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.update(connection, sql, params));
    }

    @Override
    public <T> Completable<Integer> updateObject(T object) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.updateObject(connection, object));
    }

    @Override
    public <T> Completable<T> insert(String sql, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.insert(connection, sql, params));
    }

    @Override
    public <T, R> Completable<R> insertObject(T object) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.insertObject(connection, object));
    }

    @Override
    public <T, R> Completable<R> insertObjectBatch(List<T> list, Class<T> clazz, Func1<SQLResultSet, R> handler) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.insertObjectBatch(connection, rs -> handler.call(new JDBCResultSet(rs)), clazz, list));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, R> Completable<List<R>> insertObjectBatch(List<T> list, Class<T> clazz) {
        return insertObjectBatch(list, clazz, rs -> {
            List<R> ret = new ArrayList<>();
            while (rs.next()) {
                ret.add((R) rs.getObject(1));
            }
            return ret;
        });
    }

    @Override
    public <R> Completable<R> insertBatch(String sql, Object[][] params, Func1<SQLResultSet, R> handler) {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                return helper.getRunner().insertBatch(connection, sql, rs -> handler.call(new JDBCResultSet(rs)), params);
            } catch (SQLException e) {
                throw new DBException(e);
            }
        });
    }

    @Override
    public <T> Completable<Integer> deleteById(Object id, Class<T> clazz) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.deleteById(connection, clazz, id));
    }

    @Override
    public Completable<int[]> executeBatch(String sql, Object[][] params) {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                return helper.getRunner().batch(connection, sql, params);
            } catch (SQLException e) {
                throw new DBException(e);
            }
        });
    }

    @Override
    public Completable<Void> setTransactionIsolation(TransactionIsolation transactionIsolation) {
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
    public Completable<Void> setAutoCommit(boolean autoCommit) {
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
    public Completable<Void> rollback() {
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
    public Completable<Void> commit() {
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
    public Completable<Void> close() {
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
    public Completable<Void> commitAndClose() {
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
    public Completable<Void> rollbackAndClose() {
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
    public <T> Completable<T> inTransaction(Func1<SQLConnection, Completable<T>> func1) {
        if (inTransaction.compareAndSet(false, true)) {
            Completable<T> ret = new Completable<>();
            if (getAutoCommit()) {
                setAutoCommit(false).thenAccept(c -> _inTransaction(func1, ret)).exceptionally(e -> {
                    inTransaction.set(false);
                    ret.failed(e);
                    return null;
                });
            } else {
                _inTransaction(func1, ret);
            }
            return ret;
        } else {
            return func1.call(this);
        }
    }

    private <T> void _inTransaction(Func1<SQLConnection, Completable<T>> func1, Completable<T> ret) {
        try {
            func1.call(this).thenAccept(c -> commitAndEndTransaction(ret, c)).exceptionally(e -> {
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
