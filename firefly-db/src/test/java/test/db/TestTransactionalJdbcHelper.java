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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.is;

public class TestTransactionalJdbcHelper {

    private SQLClient sqlClient;
    private int size = 10;

    public TestTransactionalJdbcHelper() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test");
        config.setDriverClassName("org.h2.Driver");
        config.setAutoCommit(false);
        HikariDataSource ds = new HikariDataSource(config);
        sqlClient = new JDBCClient(ds);
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
                       return c.insertBatch(sql, params, (rs) -> {
                           List<Integer> ids = new ArrayList<>();
                           while (rs.next()) {
                               ids.add(rs.getInt(1));
                           }
                           return ids;
                       });
                   })).thenAccept(System.out::println).get();
    }

    private <T> CompletableFuture<T> exec(Func1<SQLConnection, CompletableFuture<T>> func1) {
        return sqlClient.inTransaction(func1);
    }

    @Test
    public void test() throws Exception {
        exec(c -> {
            Completable<Void> ret = new Completable<>();
            try {
                for (long i = 1; i <= size; i++) {
                    User user = c.queryById(i, User.class).get();
                    Assert.assertThat(user.getId(), is(i));
                    Assert.assertThat(user.getName(), is("test transaction " + (i - 1)));
                    Assert.assertThat(user.getPassword(), is("pwd transaction " + (i - 1)));
                }
                ret.succeeded(null);
            } catch (Exception e) {
                e.printStackTrace();
                ret.failed(e);
            }
            return ret;
        }).get();
    }

    @Test
    public void testRollback() throws Exception {
        Long id = 1L;
        exec(c -> {
            User user = new User();
            user.setId(id);
            user.setName("apple");

            return c.updateObject(user).thenCompose(row -> {
                Assert.assertThat(row, is(1));
                return c.queryById(id, User.class);
            }).thenCompose(user1 -> {
                System.out.println("query user 1 -> " + user1);
                Assert.assertThat(user1.getName(), is("apple"));
                return c.rollback();
            });
        }).thenCompose(ret -> exec(c -> c.queryById(id, User.class)))
          .thenAccept(user -> Assert.assertThat(user.getName(), is("test transaction 0")))
          .get();
    }

//    @Test
//    public void testRollback2() {
//        int ret = jdbcHelper.executeTransaction((helper0) -> {
//            User user0 = new User();
//            user0.setId(2L);
//            user0.setName("orange");
//            int row0 = helper0.updateObject(user0);
//            Assert.assertThat(row0, is(1));
//
//            user0 = helper0.queryById(User.class, 2L);
//            System.out.println(user0);
//            Assert.assertThat(user0.getName(), is("orange"));
//
//            int r = jdbcHelper.executeTransaction((helper) -> {
//                User user1 = new User();
//                user1.setId(1L);
//                user1.setName("apple");
//                int row = helper.updateObject(user1);
//                Assert.assertThat(row, is(1));
//
//                user1 = helper.queryById(User.class, 1L);
//                System.out.println(user1);
//                Assert.assertThat(user1.getName(), is("apple"));
//                helper.getTransactionalManager().rollback();
//                return 0;
//            });
//            Assert.assertThat(r, is(0));
//            return 0;
//        });
//
//        Assert.assertThat(ret, is(0));
//
//        User user1 = jdbcHelper.queryById(User.class, 1L);
//        Assert.assertThat(user1.getName(), is("test transaction 0"));
//
//        User user2 = jdbcHelper.queryById(User.class, 2L);
//        Assert.assertThat(user2.getName(), is("test transaction 1"));
//    }

    @After
    public void after() throws Exception {
        exec(c -> c.update("DROP TABLE IF EXISTS `test`.`user`")).get();
        System.out.println("drop table user");
    }
}
