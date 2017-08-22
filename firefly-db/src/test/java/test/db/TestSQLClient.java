package test.db;

import com.firefly.db.SQLClient;
import com.firefly.db.SQLConnection;
import com.firefly.db.jdbc.JDBCClient;
import com.firefly.utils.concurrent.Promise.Completable;
import com.firefly.utils.function.Func1;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;

public class TestSQLClient {

    private SQLClient sqlClient;
    private int size = 10;

    public TestSQLClient() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test");
        config.setDriverClassName("org.h2.Driver");
        config.setAutoCommit(false);
        HikariDataSource ds = new HikariDataSource(config);
        sqlClient = new JDBCClient(ds);
    }

    private <T> CompletableFuture<T> exec(Func1<SQLConnection, CompletableFuture<T>> func1) {
        return sqlClient.newTransaction(func1);
    }

    @Before
    public void before() throws Exception {
        exec(c -> c.update("drop schema if exists test")
                   .thenCompose(v -> c.update("create schema test"))
                   .thenCompose(v -> c.update("set mode MySQL"))
                   .thenCompose(v -> c.update("CREATE TABLE `test`.`user`(id BIGINT AUTO_INCREMENT PRIMARY KEY, pt_name VARCHAR(255), pt_password VARCHAR(255), other_info VARCHAR(255))"))
                   .thenCompose(v -> {
                       Object[][] params = new Object[size][2];
                       for (int i = 0; i < size; i++) {
                           params[i][0] = "test transaction " + i;
                           params[i][1] = "pwd transaction " + i;
                       }
                       String sql = "insert into `test`.`user`(pt_name, pt_password) values(?,?)";
                       return c.insertBatch(sql, params, (rs) -> rs.stream().map(r -> r.getInt(1)).collect(Collectors.toList()));
                   })).thenAccept(System.out::println).get();
    }

    @After
    public void after() throws Exception {
        exec(c -> c.update("DROP TABLE IF EXISTS `test`.`user`")).get();
        System.out.println("drop table user");
    }

    @Test
    public void test() throws Exception {
        exec(c -> testUser(c, 1)).get();
    }

    private CompletableFuture<Long> testUser(SQLConnection c, long i) {
        return c.queryById(i, User.class).thenCompose(user -> {
            Assert.assertThat(user.getId(), is(i));
            Assert.assertThat(user.getName(), is("test transaction " + (i - 1)));
            Assert.assertThat(user.getPassword(), is("pwd transaction " + (i - 1)));
            System.out.println("query user -> " + user);
            if (i < size) {
                return testUser(c, i + 1);
            } else {
                Completable<Long> ret = new Completable<>();
                ret.succeeded(i);
                return ret;
            }
        });
    }

    @Test
    public void testRollback() throws Exception {
        Long id = 1L;
        exec(c -> {
            User user = new User();
            user.setId(id);
            user.setName("apple");
            return c.updateObject(user)
                    .thenAccept(row -> Assert.assertThat(row, is(1)))
                    .thenCompose(v -> c.queryById(id, User.class))
                    .thenAccept(user1 -> Assert.assertThat(user1.getName(), is("apple")))
                    .thenCompose(v -> c.rollback());
        }).thenCompose(ret -> exec(c -> c.queryById(id, User.class)))
          .thenAccept(user -> Assert.assertThat(user.getName(), is("test transaction 0")))
          .get();
    }

    @Test
    public void testRollback2() throws Exception {
        exec(c -> {
            User user0 = new User();
            user0.setId(2L);
            user0.setName("orange");
            return c.updateObject(user0)
                    .thenAccept(row0 -> Assert.assertThat(row0, is(1)))
                    .thenCompose(v -> c.queryById(2L, User.class))
                    .thenAccept(user -> Assert.assertThat(user.getName(), is("orange")))
                    .thenCompose(v -> c.inTransaction(c1 -> {
                        User user1 = new User();
                        user1.setId(1L);
                        user1.setName("apple");
                        return c1.updateObject(user1)
                                 .thenAccept(row1 -> Assert.assertThat(row1, is(1)))
                                 .thenCompose(v1 -> c1.queryById(1L, User.class))
                                 .thenAccept(user -> Assert.assertThat(user.getName(), is("apple")))
                                 .thenCompose(v1 -> c1.rollback());
                    }))
                    .thenCompose(v -> c.queryById(1L, User.class))
                    .thenAccept(user -> Assert.assertThat(user.getName(), is("test transaction 0")))
                    .thenCompose(v -> c.queryById(2L, User.class))
                    .thenAccept(user -> Assert.assertThat(user.getName(), is("test transaction 1")));
        }).get();
    }
}
