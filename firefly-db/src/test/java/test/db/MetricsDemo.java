package test.db;

import com.firefly.db.JDBCHelper;
import com.firefly.utils.concurrent.ThreadUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Pengtao Qiu
 */
public class MetricsDemo {

    public static void main(String[] args) {
        JDBCHelper jdbcHelper = createJDBCHelper();
        initData(jdbcHelper);

        while (true) {
            User user = jdbcHelper.queryById(User.class, 3);
            System.out.println(user);
            ThreadUtils.sleep(1000L);
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
