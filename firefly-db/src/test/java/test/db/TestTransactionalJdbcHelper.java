package test.db;

import com.firefly.db.SQLClient;
import com.firefly.db.SQLConnection;
import com.firefly.db.jdbc.JDBCClient;
import com.firefly.utils.concurrent.Promise.Completable;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
        SQLConnection connection = sqlClient.getConnection().get();
        connection.inTransaction(c -> {
            try {
                c.update("drop schema if exists test").get();
                c.update("create schema test").get();
                c.update("set mode MySQL").get();
                c.update("CREATE TABLE `test`.`user`(id BIGINT AUTO_INCREMENT PRIMARY KEY, pt_name VARCHAR(255), pt_password VARCHAR(255), other_info VARCHAR(255))").get();

                Object[][] params = new Object[size][2];
                for (int i = 0; i < size; i++) {
                    params[i][0] = "test transaction " + i;
                    params[i][1] = "pwd transaction " + i;
                }
                String sql = "insert into `test`.`user`(pt_name, pt_password) values(?,?)";
                c.executeBatch(sql, params).get();
                Completable<List<Integer>> idList = c.insertBatch(sql, params, (rs) -> {
                    List<Integer> ids = new ArrayList<>();
                    while (rs.next()) {
                        ids.add(rs.getInt(1));
                    }
                    return ids;
                });
                System.out.println(idList.get().toString());
                return idList;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }).get();
    }

    @Test
    public void test() throws Exception {
        SQLConnection connection = sqlClient.getConnection().get();
        connection.inTransaction(c -> {
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
        SQLConnection connection = sqlClient.getConnection().get();
        Long id = 1L;

        int r = connection.inTransaction(c -> {
            Completable<Integer> ret = new Completable<>();

            try {
                User user = new User();
                user.setId(1L);
                user.setName("apple");
                int row = c.updateObject(user).get();
                Assert.assertThat(row, is(1));

                User user1 = c.queryById(id, User.class).get();
                Assert.assertThat(user1.getName(), is("apple"));
                c.rollback().thenAccept(v -> ret.succeeded(0)).exceptionally(e -> {
                    ret.failed(e);
                    return null;
                });
            } catch (Exception e) {
                ret.failed(e);
            }
            return ret;
        }).get();

        Assert.assertThat(r, is(0));
        connection = sqlClient.getConnection().get();
        User user2 = connection.queryById(id, User.class).get();
        Assert.assertThat(user2.getName(), is("test transaction 0"));
        connection.close().get();
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
        SQLConnection connection = sqlClient.getConnection().get();
        connection.update("DROP TABLE IF EXISTS `test`.`user`");
        connection.close().get();
    }
}
