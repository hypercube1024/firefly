package com.firefly.db.jdbc;

import com.firefly.db.MetricReporterFactory;
import com.firefly.db.SQLClient;
import com.firefly.db.SQLConnection;
import com.firefly.db.jdbc.helper.DefaultBeanProcessor;
import com.firefly.db.jdbc.helper.JDBCHelper;
import com.firefly.utils.function.Func1;
import com.firefly.utils.lang.AbstractLifeCycle;
import org.apache.commons.dbutils.QueryRunner;

import javax.sql.DataSource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * @author Pengtao Qiu
 */
public class JDBCClient extends AbstractLifeCycle implements SQLClient {

    private final JDBCHelper jdbcHelper;

    public JDBCClient(DataSource dataSource) {
        this(dataSource, true, null);
    }

    public JDBCClient(DataSource dataSource, boolean monitorEnable, MetricReporterFactory metricReporterFactory) {
        this(dataSource,
                new QueryRunner(dataSource),
                new DefaultBeanProcessor(),
                null,
                monitorEnable,
                metricReporterFactory);
    }

    public JDBCClient(DataSource dataSource,
                      QueryRunner runner,
                      DefaultBeanProcessor defaultBeanProcessor,
                      ExecutorService executorService,
                      boolean monitorEnable,
                      MetricReporterFactory metricReporterFactory) {
        jdbcHelper = new JDBCHelper(dataSource, runner, defaultBeanProcessor, executorService, monitorEnable, metricReporterFactory);
    }

    @Override
    protected void init() {

    }

    @Override
    protected void destroy() {
        jdbcHelper.stop();
    }

    @Override
    public CompletableFuture<SQLConnection> getConnection() {
        return jdbcHelper.asyncGetConnection().thenApply(c -> new JDBCConnection(jdbcHelper, c));
    }

    @Override
    public <T> CompletableFuture<T> newTransaction(Func1<SQLConnection, CompletableFuture<T>> func1) {
        return getConnection().thenCompose(conn -> conn.inTransaction(func1));
    }
}
