package com.firefly.db;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.firefly.utils.Assert;
import com.firefly.utils.function.Func1;
import com.firefly.utils.function.Func2;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.ResultSetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class TransactionalJDBCHelper {

    private final static Logger log = LoggerFactory.getLogger("firefly-system");

    private final ThreadLocal<Transaction> transaction = new ThreadLocal<>();
    private final JDBCHelper jdbcHelper;

    public TransactionalJDBCHelper(DataSource dataSource) {
        this(new JDBCHelper(dataSource));
    }

    public TransactionalJDBCHelper(DataSource dataSource, MetricRegistry metrics, ScheduledReporter reporter) {
        this(new JDBCHelper(dataSource, metrics, reporter));
    }

    public TransactionalJDBCHelper(JDBCHelper jdbcHelper) {
        this.jdbcHelper = jdbcHelper;
    }

    public JDBCHelper getJdbcHelper() {
        return jdbcHelper;
    }

    public <T> T queryForSingleColumn(String sql, Object... params) {
        return _executeTransaction((connection, helper) -> helper.queryForSingleColumn(connection, sql, params));
    }

    public <T> T queryForObject(String sql, Class<T> t, Object... params) {
        return _executeTransaction((connection, helper) -> helper.queryForObject(connection, sql, t, params));
    }

    public <T> T queryForObject(String sql, Class<T> t, BeanProcessor beanProcessor, Object... params) {
        return _executeTransaction((connection, helper) -> helper.queryForObject(connection, sql, t, beanProcessor, params));
    }

    public <T> T queryById(Class<T> t, Object id) {
        return _executeTransaction((connection, helper) -> helper.queryById(connection, t, id));
    }

    public <K, V> Map<K, V> queryForBeanMap(String sql, Class<V> t, Object... params) {
        return _executeTransaction((connection, helper) -> helper.queryForBeanMap(connection, sql, t, params));
    }

    public <K, V> Map<K, V> queryForBeanMap(String sql, Class<V> t, BeanProcessor beanProcessor, Object... params) {
        return _executeTransaction((connection, helper) -> {
            String columnName = helper.getDefaultBeanProcessor().getIdColumnName(t);
            Assert.notNull(columnName);

            return helper.queryForBeanMap(connection, sql, t, columnName, beanProcessor, params);
        });
    }

    public <T> List<T> queryForList(String sql, Class<T> t, Object... params) {
        return _executeTransaction((connection, helper) -> helper.queryForList(connection, sql, t, params));
    }

    public <T> List<T> queryForList(String sql, Class<T> t, BeanProcessor beanProcessor, Object... params) {
        return _executeTransaction((connection, helper) -> helper.queryForList(connection, sql, t, beanProcessor, params));
    }

    public int update(String sql, Object... params) {
        Integer ret = _executeTransaction((connection, helper) -> helper.update(connection, sql, params));
        return ret != null ? ret : -1;
    }

    public int updateObject(Object object) {
        Integer ret = _executeTransaction((connection, helper) -> helper.updateObject(connection, object));
        return ret != null ? ret : -1;
    }

    public <T> T insert(String sql, Object... params) {
        return _executeTransaction((connection, helper) -> helper.insert(connection, sql, params));
    }

    public <T> T insertObject(Object object) {
        return _executeTransaction((connection, helper) -> helper.insertObject(connection, object));
    }

    public int deleteById(Class<?> t, Object id) {
        Integer ret = _executeTransaction((connection, helper) -> helper.deleteById(connection, t, id));
        return ret != null ? ret : -1;
    }

    public int[] batch(String sql, Object[][] params) {
        return _executeTransaction((connection, helper) -> {
            int[] ret;
            try {
                ret = helper.getRunner().batch(connection, sql, params);
            } catch (Exception e) {
                log.error("batch exception", e);
                throw new DBException(e);
            }
            return ret;
        });
    }

    public <T> T insertBatch(String sql, ResultSetHandler<T> rsh, Object[][] params) {
        return _executeTransaction((connection, helper) -> {
            T ret;
            try {
                ret = helper.getRunner().insertBatch(connection, sql, rsh, params);
            } catch (Exception e) {
                log.error("insert batch exception", e);
                throw new DBException(e);
            }
            return ret;
        });
    }

    public <T> T executeTransaction(Func1<TransactionalJDBCHelper, T> func) {
        beginTransaction();
        try {
            T ret = func.call(this);
            commit();
            return ret;
        } catch (Throwable t) {
            rollback();
            log.error("the transaction exception", t);
        } finally {
            endTransaction();
        }
        return null;
    }

    private <T> T _executeTransaction(Func2<Connection, JDBCHelper, T> func) {
        beginTransaction();
        try {
            T ret = func.call(getConnection(), jdbcHelper);
            commit();
            return ret;
        } catch (Throwable t) {
            rollback();
            log.error("the transaction exception", t);
        } finally {
            endTransaction();
        }
        return null;
    }

    private void beginTransaction() {
        getTransaction().beginTransaction();
    }

    public Connection getConnection() {
        return getTransaction().getConnection();
    }

    public void commit() {
        getTransaction().commit();
    }

    public void rollback() {
        getTransaction().rollback();
    }

    private void endTransaction() {
        getTransaction().endTransaction();
    }

    private Transaction getTransaction() {
        Transaction t = transaction.get();
        if (t == null) {
            t = new Transaction();
            transaction.set(t);
        }
        return t;
    }

    enum Status {
        INIT, START, COMMIT, ROLLBACK, END
    }

    class Transaction {
        private Connection connection;
        private Status status = Status.INIT;
        private int count = 0;

        synchronized void beginTransaction() {
            if (status == Status.INIT) {
                connection = jdbcHelper.getConnection();
                jdbcHelper.setAutoCommit(connection, false);
                status = Status.START;
            }
            count++;
            log.debug("begin transaction {}", count);
        }

        synchronized Connection getConnection() {
            check();
            return connection;
        }

        synchronized void rollback() {
            check();
            status = Status.ROLLBACK;
        }

        synchronized void commit() {
            check();
            if (status != Status.ROLLBACK) {
                status = Status.COMMIT;
            }
        }

        private synchronized void check() {
            if (status == Status.INIT) {
                throw new IllegalStateException("The transaction has not started, " + status);
            }
            if (status == Status.END) {
                throw new IllegalStateException("The transaction has ended, " + status);
            }
        }

        synchronized void endTransaction() {
            count--;
            if (count == 0) {
                switch (status) {
                    case START:
                    case COMMIT:
                        jdbcHelper.commit(connection);
                        break;
                    case ROLLBACK:
                        jdbcHelper.rollback(connection);
                        break;
                    default:
                        break;
                }

                jdbcHelper.close(connection);
                transaction.set(null);
                status = Status.END;
            }
            log.debug("end transaction {}", count);
        }

    }

}
