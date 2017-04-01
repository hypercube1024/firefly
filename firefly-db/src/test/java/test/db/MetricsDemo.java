package test.db;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;
import com.firefly.db.JDBCHelper;
import com.firefly.utils.concurrent.ThreadUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class MetricsDemo {

    public static void main(String[] args) {
        MetricRegistry metrics = new MetricRegistry();
        ScheduledReporter reporter = Slf4jReporter.forRegistry(metrics)
                                                  .outputTo(LoggerFactory.getLogger("firefly-monitor"))
                                                  .convertRatesTo(TimeUnit.SECONDS)
                                                  .convertDurationsTo(TimeUnit.MILLISECONDS)
                                                  .build();
        reporter.start(5, TimeUnit.SECONDS);

        int j = 0;
        while(true) {
            int i = j;
            Gauge<String> gauge = metrics.register("test_value_" + i + ":", () -> "hello -> " + i);
            j++;
            ThreadUtils.sleep(2000L);
        }

    }

    public static void main2(String[] args) {
        JDBCHelper jdbcHelper = createJDBCHelper();
        initData(jdbcHelper);

        while (true) {
            User user = jdbcHelper.queryById(User.class, 3);
            System.out.println(user);
            ThreadUtils.sleep(2000L);
        }
    }

    static JDBCHelper createJDBCHelper() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test");
        config.setDriverClassName("org.h2.Driver");
        config.setAutoCommit(false);
        HikariDataSource ds = new HikariDataSource(config);
        return new JDBCHelper(ds);
    }

    static void initData(JDBCHelper jdbcHelper) {
        int size = 10;
        jdbcHelper.update("drop schema if exists test");
        jdbcHelper.update("create schema test");
        jdbcHelper.update("set mode MySQL");
        jdbcHelper.update(
                "CREATE TABLE `test`.`user`(id BIGINT AUTO_INCREMENT PRIMARY KEY, pt_name VARCHAR(255), pt_password VARCHAR(255), other_info VARCHAR(255))");

        for (int i = 1; i <= size; i++) {
            jdbcHelper.insert("insert into `test`.`user`(pt_name, pt_password) values(?,?)", "test" + i, "test_pwd" + i);
        }
    }
}
