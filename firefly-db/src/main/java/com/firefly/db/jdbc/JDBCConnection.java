package com.firefly.db.jdbc;

import com.firefly.db.DBException;
import com.firefly.db.SQLConnection;
import com.firefly.db.SQLResultSet;
import com.firefly.db.TransactionIsolation;
import com.firefly.db.jdbc.helper.JDBCHelper;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Func1;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class JDBCConnection implements SQLConnection {

    private final JDBCHelper jdbcHelper;
    private final Connection connection;

    public JDBCConnection(JDBCHelper jdbcHelper, Connection connection) {
        this.jdbcHelper = jdbcHelper;
        this.connection = connection;
    }

    public JDBCHelper getJdbcHelper() {
        return jdbcHelper;
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public <T> Promise.Completable<T> queryForSingleColumn(String sql, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForSingleColumn(connection, sql, params));
    }

    @Override
    public <T> Promise.Completable<T> queryForObject(String sql, Class<T> clazz, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForObject(connection, sql, clazz, params));
    }

    @Override
    public <T> Promise.Completable<T> queryById(Object id, Class<T> clazz) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryById(connection, clazz, id));
    }

    @Override
    public <K, V> Promise.Completable<Map<K, V>> queryForBeanMap(String sql, Class<V> valueClass, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForBeanMap(connection, sql, valueClass, params));
    }

    @Override
    public <T> Promise.Completable<List<T>> queryForList(String sql, Class<T> clazz, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.queryForList(connection, sql, clazz, params));
    }

    @Override
    public <T> Promise.Completable<T> query(String sql, Func1<SQLResultSet, T> handler, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                return helper.getRunner().query(connection, sql, rs -> handler.call(new JDBCResultSet(rs)), params);
            } catch (SQLException e) {
                throw new DBException(e);
            }
        });
    }

    @Override
    public Promise.Completable<Integer> update(String sql, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.update(connection, sql, params));
    }

    @Override
    public <T> Promise.Completable<Integer> updateObject(T object) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.updateObject(connection, object));
    }

    @Override
    public <T> Promise.Completable<T> insert(String sql, Object... params) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.insert(connection, sql, params));
    }

    @Override
    public <T, R> Promise.Completable<R> insertObject(T object) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.insertObject(connection, object));
    }

    @Override
    public <T, R> Promise.Completable<R> insertObjectBatch(List<T> list, Class<T> clazz, Func1<SQLResultSet, R> handler) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.insertObjectBatch(connection, rs -> handler.call(new JDBCResultSet(rs)), clazz, list));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T, R> Promise.Completable<List<R>> insertObjectBatch(List<T> list, Class<T> clazz) {
        return insertObjectBatch(list, clazz, rs -> {
            List<R> ret = new ArrayList<>();
            while (rs.next()) {
                ret.add((R) rs.getObject(1));
            }
            return ret;
        });
    }

    @Override
    public <T> Promise.Completable<Integer> deleteById(Object id, Class<T> clazz) {
        return jdbcHelper.async(connection, (conn, helper) -> helper.deleteById(connection, clazz, id));
    }

    @Override
    public Promise.Completable<int[]> executeBatch(String sql, Object[][] params) {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                return helper.getRunner().batch(connection, sql, params);
            } catch (SQLException e) {
                throw new DBException(e);
            }
        });
    }

    @Override
    public Promise.Completable<Void> setTransactionIsolation(TransactionIsolation transactionIsolation) {
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
    public Promise.Completable<Void> setAutoCommit(boolean autoCommit) {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                connection.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                throw new DBException(e);
            }
            return null;
        });
    }

    @Override
    public Promise.Completable<Void> rollback() {
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
    public Promise.Completable<Void> commit() {
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
    public Promise.Completable<Void> close() {
        return jdbcHelper.async(connection, (conn, helper) -> {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new DBException(e);
            }
            return null;
        });
    }
}
