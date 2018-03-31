package com.firefly.db.jdbc.helper;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.firefly.db.*;
import com.firefly.db.jdbc.helper.DefaultBeanProcessor.Mapper;
import com.firefly.db.jdbc.helper.DefaultBeanProcessor.SQLMapper;
import com.firefly.utils.Assert;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ServiceUtils;
import com.firefly.utils.classproxy.JavassistClassProxyFactory;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Func2;
import com.firefly.utils.lang.AbstractLifeCycle;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.firefly.db.jdbc.helper.JDBCConnectionUtils.*;

public class JDBCHelper extends AbstractLifeCycle {

    private final static Logger log = LoggerFactory.getLogger("firefly-system");

    private final DataSource dataSource;
    private final QueryRunner runner;
    private final DefaultBeanProcessor defaultBeanProcessor;
    private final ExecutorService executorService;
    private final boolean monitorEnable;
    private final MetricReporterFactory metricReporterFactory;
    private final LatencyTopTracker latencyTopTracker;

    public JDBCHelper(DataSource dataSource) {
        this(dataSource, true, null);
    }

    public JDBCHelper(DataSource dataSource, boolean monitorEnable, MetricReporterFactory metricReporterFactory) {
        this(dataSource,
                new QueryRunner(dataSource),
                new DefaultBeanProcessor(),
                null,
                monitorEnable,
                metricReporterFactory);
    }

    public JDBCHelper(DataSource dataSource,
                      QueryRunner runner,
                      DefaultBeanProcessor defaultBeanProcessor,
                      ExecutorService executorService,
                      boolean monitorEnable,
                      MetricReporterFactory metricReporterFactory) {

        if (metricReporterFactory != null) {
            this.metricReporterFactory = metricReporterFactory;
        } else {
            this.metricReporterFactory = ServiceUtils.loadService(MetricReporterFactory.class, new DefaultMetricReporterFactory());
        }

        latencyTopTracker = new LatencyTopTracker(getMetrics());

        this.dataSource = dataSource;
        if (monitorEnable) {
            this.runner = getMonitorQueryRunner(runner);
        } else {
            this.runner = runner;
        }
        this.defaultBeanProcessor = defaultBeanProcessor;
        this.monitorEnable = monitorEnable;
        if (executorService != null) {
            this.executorService = executorService;
        } else {
            this.executorService = new ThreadPoolExecutor(16, 300,
                    30L, TimeUnit.SECONDS,
                    new LinkedTransferQueue<>(),
                    r -> new Thread(r, "firefly-JDBC-helper"));
        }
        start();
    }

    private QueryRunner getMonitorQueryRunner(QueryRunner queryRunner) {
        try {
            return JavassistClassProxyFactory.INSTANCE.createProxy(queryRunner,
                    (handler, originalInstance, args) -> {
                        String sql = "";
                        Object[] params = null;
                        Object[] batchParams = null;
                        if (args != null && args.length > 0) {
                            for (Object arg : args) {
                                if (arg instanceof String) {
                                    sql = (String) arg;
                                } else if (arg instanceof Object[][]) {
                                    batchParams = (Object[][]) arg;
                                } else if (arg instanceof Object[]) {
                                    params = (Object[]) arg;
                                }
                            }
                        }
                        long start = System.currentTimeMillis();
                        Exception exception = null;
                        Object ret;
                        try {
                            ret = handler.invoke(originalInstance, args);
                        } catch (Exception e) {
                            exception = e;
                            throw new DBException(exception);
                        } finally {
                            long currentTime = System.currentTimeMillis();
                            long latencyTime = currentTime - start;
                            latencyTopTracker.update(sql, exception, currentTime, latencyTime);
                            if (log.isDebugEnabled()) {
                                StringBuilder str = new StringBuilder(sql);
                                if (batchParams != null) {
                                    str.append("|").append(Arrays.deepToString(batchParams));
                                } else if (params != null) {
                                    str.append("|").append(Arrays.toString(params));
                                }
                                str.append("|").append(latencyTime);
                                log.debug(str.toString());
                            }
                        }
                        return ret;
                    }, null);
        } catch (Throwable t) {
            log.error("create QueryRunner proxy exception", t);
            return queryRunner;
        }
    }

    public boolean isMonitorEnable() {
        return monitorEnable;
    }

    public MetricRegistry getMetrics() {
        return metricReporterFactory.getMetricRegistry();
    }

    public ScheduledReporter getReporter() {
        return metricReporterFactory.getScheduledReporter();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public QueryRunner getRunner() {
        return runner;
    }

    public DefaultBeanProcessor getDefaultBeanProcessor() {
        return defaultBeanProcessor;
    }

    public <T> T queryForSingleColumn(String sql, Object... params) {
        try (Connection connection = dataSource.getConnection()) {
            T ret = this.queryForSingleColumn(connection, sql, params);
            connection.commit();
            return ret;
        } catch (SQLException e) {
            log.error("query exception, sql: {}", e, sql);
            throw new DBException(e);
        }
    }

    public <T> T queryForSingleColumn(Connection connection, String sql, Object... params) {
        try {
            return Optional.<T>ofNullable(runner.query(connection, sql, new ScalarHandler<>(), params)).orElseThrow(RecordNotFound::new);
        } catch (SQLException e) {
            log.error("query exception, sql: {}", e, sql);
            throw new DBException(e);
        }
    }

    public <T> T queryForObject(String sql, Class<T> t, Object... params) {
        return this.queryForObject(sql, t, defaultBeanProcessor, params);
    }

    public <T> T queryForObject(String sql, Class<T> t, BeanProcessor beanProcessor, Object... params) {
        try (Connection connection = dataSource.getConnection()) {
            T ret = this.queryForObject(connection, sql, t, beanProcessor, params);
            connection.commit();
            return ret;
        } catch (SQLException e) {
            log.error("query exception, sql: {}", e, sql);
            throw new DBException(e);
        }
    }

    public <T> T queryById(Class<T> t, Object id) {
        try (Connection connection = dataSource.getConnection()) {
            T ret = queryById(connection, t, id);
            connection.commit();
            return ret;
        } catch (SQLException e) {
            log.error("query exception", e);
            throw new DBException(e);
        }
    }

    public <T> T queryById(Connection connection, Class<T> t, Object id) {
        SQLMapper sqlMapper = defaultBeanProcessor.generateQuerySQL(t);
        Assert.notNull(sqlMapper, "sql mapper must not be null");
        return this.queryForObject(connection, sqlMapper.sql, t, id);
    }

    public <T> T queryForObject(Connection connection, String sql, Class<T> t, Object... params) {
        return this.queryForObject(connection, sql, t, defaultBeanProcessor, params);
    }

    public <T> T queryForObject(Connection connection, String sql, Class<T> t, BeanProcessor beanProcessor,
                                Object... params) {
        try {
            return Optional.ofNullable(runner.query(connection, sql, new BeanHandler<>(t, new BasicRowProcessor(beanProcessor)), params)).orElseThrow(RecordNotFound::new);
        } catch (SQLException e) {
            log.error("query exception, sql: {}", e, sql);
            throw new DBException(e);
        }
    }

    public <K, V> Map<K, V> queryForBeanMap(String sql, Class<V> t, Object... params) {
        return this.queryForBeanMap(sql, t, defaultBeanProcessor, params);
    }

    public <K, V> Map<K, V> queryForBeanMap(String sql, Class<V> t, BeanProcessor beanProcessor, Object... params) {
        String columnName = defaultBeanProcessor.getIdColumnName(t);
        Assert.notNull(columnName);
        try (Connection connection = dataSource.getConnection()) {
            Map<K, V> ret = this.queryForBeanMap(connection, sql, t, columnName, beanProcessor, params);
            connection.commit();
            return ret;
        } catch (SQLException e) {
            log.error("query exception, sql: {}", e, sql);
            throw new DBException(e);
        }
    }

    public <K, V> Map<K, V> queryForBeanMap(Connection connection, String sql, Class<V> t, Object... params) {
        String columnName = defaultBeanProcessor.getIdColumnName(t);
        Assert.notNull(columnName);
        return this.queryForBeanMap(connection, sql, t, columnName, defaultBeanProcessor, params);
    }

    public <K, V> Map<K, V> queryForBeanMap(Connection connection, String sql, Class<V> t, String columnName,
                                            BeanProcessor beanProcessor, Object... params) {
        try {
            return Optional.ofNullable(runner.query(
                    connection, sql,
                    new DefaultBeanMapHandler<K, V>(t, new BasicRowProcessor(beanProcessor), 0, columnName),
                    params)).orElse(Collections.emptyMap());
        } catch (SQLException e) {
            log.error("query exception, sql: {}", e, sql);
            throw new DBException(e);
        }
    }

    public <T> List<T> queryForList(String sql, Class<T> t, Object... params) {
        return this.queryForList(sql, t, defaultBeanProcessor, params);
    }

    public <T> List<T> queryForList(String sql, Class<T> t, BeanProcessor beanProcessor, Object... params) {
        try (Connection connection = dataSource.getConnection()) {
            List<T> ret = this.queryForList(connection, sql, t, beanProcessor, params);
            connection.commit();
            return ret;
        } catch (SQLException e) {
            log.error("query exception, sql: {}", e, sql);
            throw new DBException(e);
        }
    }

    public <T> List<T> queryForList(Connection connection, String sql, Class<T> t, Object... params) {
        return this.queryForList(connection, sql, t, defaultBeanProcessor, params);
    }

    public <T> List<T> queryForList(Connection connection, String sql, Class<T> t, BeanProcessor beanProcessor, Object... params) {
        try {
            return Optional.ofNullable(runner.query(connection, sql, new BeanListHandler<>(t, new BasicRowProcessor(beanProcessor)), params))
                           .orElse(Collections.emptyList());
        } catch (SQLException e) {
            log.error("query exception, sql: {}", e, sql);
            throw new DBException(e);
        }
    }

    public int update(String sql, Object... params) {
        try (Connection connection = dataSource.getConnection()) {
            int ret = this.update(connection, sql, params);
            connection.commit();
            return ret;
        } catch (SQLException e) {
            log.error("update exception, sql: {}", e, sql);
            throw new DBException(e);
        }
    }

    public int updateObject(Object object) {
        try (Connection connection = dataSource.getConnection()) {
            int ret = this.updateObject(connection, object);
            connection.commit();
            return ret;
        } catch (SQLException e) {
            log.error("update exception", e);
            throw new DBException(e);
        }
    }

    public int updateObject(Connection connection, Object object) {
        Class<?> t = object.getClass();
        SQLMapper sqlMapper = defaultBeanProcessor.generateUpdateSQL(t, object);
        Assert.notNull(sqlMapper, "the sql mapper must not be null");
        Assert.notEmpty(sqlMapper.propertyMap, "the property map must not be empty");

        Object[] params = new Object[sqlMapper.propertyMap.size()];
        sqlMapper.propertyMap.forEach((property, index) -> {
            try {
                Object value = ReflectUtils.get(object, property);
                params[index] = value;
            } catch (Throwable ignored) {
            }
        });
        return this.update(connection, sqlMapper.sql, params);
    }

    public int update(Connection connection, String sql, Object... params) {
        try {
            return runner.update(connection, sql, params);
        } catch (SQLException e) {
            log.error("update exception, sql: {}", e, sql);
            throw new DBException(e);
        }
    }

    public <T> T insert(String sql, Object... params) {
        try (Connection connection = dataSource.getConnection()) {
            T ret = this.insert(connection, sql, params);
            connection.commit();
            return ret;
        } catch (SQLException e) {
            log.error("insert exception, sql: {}", e, sql);
            throw new DBException(e);
        }
    }

    public <T> T insertObject(Object object) {
        try (Connection connection = dataSource.getConnection()) {
            T ret = this.insertObject(connection, object);
            connection.commit();
            return ret;
        } catch (SQLException e) {
            log.error("insert exception", e);
            throw new DBException(e);
        }

    }

    public <T> T insertObject(Connection connection, Object object) {
        Class<?> t = object.getClass();
        SQLMapper sqlMapper = defaultBeanProcessor.generateInsertSQL(t);
        Assert.notNull(sqlMapper, "the sql mapper must not be null");
        Assert.notEmpty(sqlMapper.propertyMap, "the property map must not be empty");

        Object[] params = new Object[sqlMapper.propertyMap.size()];
        sqlMapper.propertyMap.forEach((property, index) -> {
            try {
                Object value = ReflectUtils.get(object, property);
                params[index] = value;
            } catch (Throwable ignored) {
            }
        });
        T ret = this.insert(connection, sqlMapper.sql, params);
        if (ret != null) {
            Mapper idMapper = defaultBeanProcessor.getIdMapper(t);
            if (idMapper != null) {
                try {
                    ReflectUtils.set(object, idMapper.propertyName, ret);
                } catch (Throwable ignored) {
                }
            }
        }
        return ret;
    }

    public <T, R> R insertObjectBatch(Connection connection, ResultSetHandler<R> rsh, Class<T> t, List<T> list) {
        SQLMapper sqlMapper = defaultBeanProcessor.generateInsertSQL(t);
        Assert.notNull(sqlMapper, "the sql mapper must not be null");
        Assert.notEmpty(sqlMapper.propertyMap, "the property map must not be empty");

        Object[][] params = new Object[list.size()][sqlMapper.propertyMap.size()];
        for (int i = 0; i < list.size(); i++) {
            Object object = list.get(i);
            final int j = i;
            sqlMapper.propertyMap.forEach((property, index) -> {
                try {
                    params[j][index] = ReflectUtils.get(object, property);
                } catch (Throwable ignored) {
                }
            });
        }
        try {
            return getRunner().insertBatch(connection, sqlMapper.sql, rsh, params);
        } catch (SQLException e) {
            log.error("insert batch exception", e);
            throw new DBException(e);
        }
    }

    public <T> T insert(Connection connection, String sql, Object... params) {
        try {
            return runner.insert(connection, sql, new ScalarHandler<T>(), params);
        } catch (SQLException e) {
            log.error("insert exception, sql: {}", e, sql);
            throw new DBException(e);
        }
    }

    public int deleteById(Class<?> t, Object id) {
        try (Connection connection = dataSource.getConnection()) {
            int ret = this.deleteById(connection, t, id);
            connection.commit();
            return ret;
        } catch (SQLException e) {
            log.error("delete exception", e);
            throw new DBException(e);
        }
    }

    public int deleteById(Connection connection, Class<?> t, Object id) {
        SQLMapper sqlMapper = defaultBeanProcessor.generateDeleteSQL(t);
        Assert.notNull(sqlMapper, "sql mapper must not be null");
        return this.update(connection, sqlMapper.sql, id);
    }

    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            log.error("get connection exception", e);
            throw new DBException(e);
        }
    }


    public <T> T executeTransaction(Func2<Connection, JDBCHelper, T> func) {
        Connection connection = getConnection();
        setAutoCommit(connection, false);

        try {
            T ret = func.call(connection, this);
            commit(connection);
            return ret;
        } catch (Throwable t) {
            rollback(connection);
            log.error("the transaction exception", t);
        } finally {
            setAutoCommit(connection, true);
            close(connection);
        }
        return null;
    }

    public <T> Promise.Completable<T> async(Connection connection, Func2<Connection, JDBCHelper, T> func) {
        Promise.Completable<T> c = new Promise.Completable<>();
        executorService.submit(() -> {
            try {
                c.succeeded(func.call(connection, this));
            } catch (Throwable t) {
                c.failed(t);
            }
        });
        return c;
    }

    public Promise.Completable<Connection> asyncGetConnection() {
        Promise.Completable<Connection> c = new Promise.Completable<>();
        executorService.submit(() -> {
            try {
                c.succeeded(getConnection());
            } catch (Throwable t) {
                c.failed(t);
            }
        });
        return c;
    }

    public <T> Promise.Completable<T> asyncTransaction(Func2<Connection, JDBCHelper, T> func) {
        Promise.Completable<T> c = new Promise.Completable<>();
        executorService.submit(() -> {
            try {
                c.succeeded(executeTransaction(func));
            } catch (Throwable t) {
                c.failed(t);
            }
        });
        return c;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    protected void init() {
        if (monitorEnable) {
            try {
                getReporter().start(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("start metric reporter exception -> {}", e.getMessage());
            }
        }
    }

    @Override
    protected void destroy() {
        try {
            executorService.shutdown();
        } catch (Exception e) {
            log.error("jdbc helper thread pool shutdown exception -> {}", e.getMessage());
        }
        if (monitorEnable) {
            try {
                getReporter().stop();
            } catch (Exception e) {
                log.error("stop metric reporter exception -> {}", e.getMessage());
            }
        }
    }
}
