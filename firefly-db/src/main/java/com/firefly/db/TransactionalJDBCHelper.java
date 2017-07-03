package com.firefly.db;

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

    private final JDBCHelper jdbcHelper;
    private final TransactionalManager transactionalManager;

    public TransactionalJDBCHelper(DataSource dataSource) {
        this(dataSource, null, new ThreadLocalTransactionalManager(dataSource));
    }

    public TransactionalJDBCHelper(DataSource dataSource, MetricReporterFactory metricReporterFactory, TransactionalManager transactionalManager) {
        this(new JDBCHelper(dataSource, metricReporterFactory), transactionalManager);
    }

    public TransactionalJDBCHelper(JDBCHelper jdbcHelper, TransactionalManager transactionalManager) {
        this.jdbcHelper = jdbcHelper;
        this.transactionalManager = transactionalManager;
    }

    public JDBCHelper getJdbcHelper() {
        return jdbcHelper;
    }

    public TransactionalManager getTransactionalManager() {
        return transactionalManager;
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
        transactionalManager.beginTransaction();
        try {
            T ret = func.call(this);
            transactionalManager.commit();
            return ret;
        } catch (Throwable t) {
            transactionalManager.rollback();
            log.error("the transaction exception", t);
            return null;
        } finally {
            transactionalManager.endTransaction();
        }
    }

    private <T> T _executeTransaction(Func2<Connection, JDBCHelper, T> func) {
        transactionalManager.beginTransaction();
        try {
            T ret = func.call(transactionalManager.getConnection(), jdbcHelper);
            transactionalManager.commit();
            return ret;
        } catch (Throwable t) {
            transactionalManager.rollback();
            log.error("the transaction exception", t);
            return null;
        } finally {
            transactionalManager.endTransaction();
        }
    }


}
