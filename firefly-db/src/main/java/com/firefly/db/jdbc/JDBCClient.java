package com.firefly.db.jdbc;

import com.firefly.db.MetricReporterFactory;
import com.firefly.db.SQLClient;
import com.firefly.db.SQLConnection;
import com.firefly.db.jdbc.helper.DefaultBeanProcessor;
import com.firefly.db.jdbc.helper.JDBCHelper;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.lang.AbstractLifeCycle;
import org.apache.commons.dbutils.QueryRunner;

import javax.sql.DataSource;
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
    public Promise.Completable<SQLConnection> getConnection() {
        Promise.Completable<SQLConnection> completable = new Promise.Completable<>();
        jdbcHelper.asyncGetConnection()
                  .thenAccept(c -> completable.succeeded(new JDBCConnection(jdbcHelper, c)))
                  .exceptionally(e -> {
                      completable.failed(e);
                      return null;
                  });
        return completable;
    }
}
