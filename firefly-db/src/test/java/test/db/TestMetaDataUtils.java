package test.db;

import com.firefly.db.jdbc.helper.JDBCHelper;
import com.firefly.db.jdbc.utils.MetaDataUtils;
import com.firefly.db.jdbc.utils.SourceCode;
import com.firefly.db.jdbc.utils.TableMetaData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.List;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestMetaDataUtils {

    private JDBCHelper jdbcHelper;
    private MetaDataUtils metaDataUtils;

    public TestMetaDataUtils() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test");
        config.setDriverClassName("org.h2.Driver");
        config.setAutoCommit(false);
        DataSource dataSource = new HikariDataSource(config);
        metaDataUtils = new MetaDataUtils(dataSource);
        jdbcHelper = new JDBCHelper(dataSource);
    }

    @Before
    public void before() {
        jdbcHelper.update("drop schema if exists test");
        jdbcHelper.update("create schema test");
        jdbcHelper.update("set mode MySQL");
        jdbcHelper.update("CREATE TABLE `test`.`hello_user`(" +
                "id BIGINT(20) AUTO_INCREMENT PRIMARY KEY, " +
                "pt_name VARCHAR(255), " +
                "pt_password VARCHAR(255), " +
                "create_time DATETIME, " +
                "status INT)");
        jdbcHelper.update("CREATE TABLE `test`.`hello_user_ext`(" +
                "id BIGINT(20) AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT(20), " +
                "create_time DATETIME, " +
                "other_info VARCHAR(255))");
    }

    @Test
    public void test() {
        List<TableMetaData> list = metaDataUtils.listTableMetaData("test", "%", "hello_%");
        System.out.println(list);
        Assert.assertThat(list.size(), is(2));

        List<SourceCode> codes = metaDataUtils.toPojo(list, "hello_", "com.hello.test");
        codes.forEach(System.out::println);
        Assert.assertThat(codes.get(0).getName(), is("User"));
        Assert.assertThat(codes.get(0).getCodes(), is("package com.hello.test;\r\n" +
                "\r\n" +
                "import lombok.Data;\r\n" +
                "\r\n" +
                "import java.io.Serializable;\r\n" +
                "\r\n" +
                "@Data\r\n" +
                "public class User implements Serializable {\r\n" +
                "\r\n" +
                "    private static final long serialVersionUID = 1L;\r\n" +
                "\r\n" +
                "    private Long id;\r\n" +
                "    private String ptName;\r\n" +
                "    private String ptPassword;\r\n" +
                "    private java.util.Date createTime;\r\n" +
                "    private Integer status;\r\n" +
                "}"));

        Assert.assertThat(codes.get(1).getName(), is("UserExt"));
        Assert.assertThat(codes.get(1).getCodes(), is("package com.hello.test;\r\n" +
                "\r\n" +
                "import lombok.Data;\r\n" +
                "\r\n" +
                "import java.io.Serializable;\r\n" +
                "\r\n" +
                "@Data\r\n" +
                "public class UserExt implements Serializable {\r\n" +
                "\r\n" +
                "    private static final long serialVersionUID = 1L;\r\n" +
                "\r\n" +
                "    private Long id;\r\n" +
                "    private Long userId;\r\n" +
                "    private java.util.Date createTime;\r\n" +
                "    private String otherInfo;\r\n" +
                "}"));
    }
}
