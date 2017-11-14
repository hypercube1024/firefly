package com.firefly.reactive.adapter.db;

import com.firefly.db.RecordNotFound;
import com.firefly.db.jdbc.JDBCClient;
import com.firefly.reactive.adapter.Reactor;
import com.firefly.utils.function.Func1;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestReactiveSQLClient {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private ReactiveSQLClient sqlClient;
    private int size = 10;

    public TestReactiveSQLClient() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test");
        config.setDriverClassName("org.h2.Driver");
        config.setAutoCommit(false);
        HikariDataSource ds = new HikariDataSource(config);
        sqlClient = Reactor.db.fromSQLClient(new JDBCClient(ds));
    }

    private <T> Mono<T> exec(Func1<ReactiveSQLConnection, Mono<T>> func1) {
        return sqlClient.newTransaction(func1);
    }

    @Before
    public void before() {
        exec(c -> c.update("drop schema if exists test")
                   .flatMap(v -> c.update("create schema test"))
                   .flatMap(v -> c.update("set mode MySQL"))
                   .flatMap(v -> c.update("CREATE TABLE `test`.`user`(id BIGINT AUTO_INCREMENT PRIMARY KEY, pt_name VARCHAR(255), pt_password VARCHAR(255), other_info VARCHAR(255))"))
                   .flatMap(v -> {
                       Object[][] params = new Object[size][2];
                       for (int i = 0; i < size; i++) {
                           params[i][0] = "test transaction " + i;
                           params[i][1] = "pwd transaction " + i;
                       }
                       String sql = "insert into `test`.`user`(pt_name, pt_password) values(?,?)";
                       return c.insertBatch(sql, params, (rs) -> rs.stream().map(r -> r.getInt(1)).collect(Collectors.toList()));
                   })).doOnSuccess(System.out::println).block();
    }

    @After
    public void after() {
        exec(c -> c.update("DROP TABLE IF EXISTS `test`.`user`")).block();
        System.out.println("drop table user");
    }

    @Test
    public void test() {
        exec(c -> testUser(c, 1)).block();
    }

    private Mono<Long> testUser(ReactiveSQLConnection c, long i) {
        return c.queryById(i, User.class).flatMap(user -> {
            Assert.assertThat(user.getId(), is(i));
            Assert.assertThat(user.getName(), is("test transaction " + (i - 1)));
            Assert.assertThat(user.getPassword(), is("pwd transaction " + (i - 1)));
            System.out.println("query user -> " + user);
            if (i < size) {
                return testUser(c, i + 1);
            } else {
                return Mono.create(monoSink -> monoSink.success(i));
            }
        });
    }

    @Test
    public void testRollback() {
        Long id = 1L;
        User user = new User();
        user.setId(id);
        user.setName("apple");
        StepVerifier.create(exec(c -> c.updateObject(user)
                                       .doOnSuccess(row -> Assert.assertThat(row, is(1)))
                                       .flatMap(v -> c.queryById(id, User.class))
                                       .doOnSuccess(user1 -> Assert.assertThat(user1.getName(), is("apple")))
                                       .flatMap(v -> c.rollback())))
                    .expectNext(true)
                    .expectComplete().verify();
        StepVerifier.create(exec(c -> c.queryById(id, User.class)).map(User::getName))
                    .expectNext("test transaction 0")
                    .verifyComplete();
    }

    @Test
    public void testRollback2() {
        exec(c -> {
            User user0 = new User();
            user0.setId(2L);
            user0.setName("orange");
            return c.updateObject(user0)
                    .doOnSuccess(row0 -> Assert.assertThat(row0, is(1)))
                    .flatMap(v -> c.queryById(2L, User.class))
                    .doOnSuccess(user -> Assert.assertThat(user.getName(), is("orange")))
                    .flatMap(v -> c.inTransaction(c1 -> {
                        User user1 = new User();
                        user1.setId(1L);
                        user1.setName("apple");
                        return c1.updateObject(user1)
                                 .doOnSuccess(row1 -> Assert.assertThat(row1, is(1)))
                                 .flatMap(v1 -> c1.queryById(1L, User.class))
                                 .doOnSuccess(user -> Assert.assertThat(user.getName(), is("apple")))
                                 .flatMap(v1 -> c1.rollback());
                    }))
                    .flatMap(v -> c.queryById(1L, User.class))
                    .doOnSuccess(user -> Assert.assertThat(user.getName(), is("test transaction 0")))
                    .flatMap(v -> c.queryById(2L, User.class))
                    .doOnSuccess(user -> Assert.assertThat(user.getName(), is("test transaction 1")));
        }).block();
    }

    @Test
    public void testExecSQL() {
        Phaser phaser = new Phaser(2);
        User user0 = new User();
        user0.setId(1L);
        user0.setName("hello");
        exec(c -> c.inTransaction(conn -> updateUser(c, user0))).subscribe(data -> phaser.arrive(), ex -> handleException(phaser));
        phaser.arriveAndAwaitAdvance();
    }

    private Mono<User> updateUser(ReactiveSQLConnection c, User user0) {
        return c.updateObject(user0)
                .doOnSuccess(row0 -> Assert.assertThat(row0, is(1)))
                .flatMap(v1 -> c.queryById(1L, User.class))
                .doOnSuccess(user -> Assert.assertThat(user.getName(), is("hello")))
                .doOnNext(v1 -> {
                    throw new RuntimeException("test exception rollback");
                })
                .doOnError(e -> {
                    log.error("test rollback 1, {}", e.getMessage());
                    Assert.assertThat(e.getMessage(), is("test exception rollback"));
                });
    }

    private Disposable handleException(Phaser phaser) {
        return sqlClient.getConnection()
                        .flatMap(c -> c.inTransaction(conn -> c.queryById(1L, User.class).doOnSuccess(user -> Assert.assertThat(user.getName(), is("test transaction 0")))))
                        .subscribe(user -> phaser.arrive());
    }

    @Test
    public void testUsers() {
        Mono<List<Long>> ids = exec(c -> c.queryForList("select * from test.user", User.class)
                                          .flatMapIterable(users -> users)
                                          .filter(user -> user.getId() % 2 == 0)
                                          .map(User::getId)
                                          .collectList());
        StepVerifier.create(ids).assertNext(idList -> {
            Assert.assertThat(idList.size(), is(5));
            System.out.println(idList);
        }).verifyComplete();
    }

    @Test
    public void testQueryById() {
        Mono<User> user = exec(c -> c.queryById(1, User.class));
        StepVerifier.create(user)
                    .assertNext(u -> Assert.assertThat(u.getName(), is("test transaction 0")))
                    .verifyComplete();
    }

    @Test
    public void testRecordNotFound() {
        Mono<User> user = exec(c -> c.queryById(size + 10, User.class));
        StepVerifier.create(user)
                    .expectErrorMatches(t -> t.getCause() instanceof RecordNotFound)
                    .verify();
    }

    @Test
    public void testInsertUser() {
        User user = new User();
        user.setName("test insert");
        user.setPassword("test insert pwd");
        Mono<Long> newUserId = exec(c -> c.insertObject(user));
        StepVerifier.create(newUserId).expectNext(size + 1L).verifyComplete();
    }

    @Test
    public void testBatchInsertUsers() {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setName("test insert " + i);
            user.setPassword("test insert pwd " + i);
            users.add(user);
        }
        Mono<List<Long>> newUserIdList = exec(c -> c.insertObjectBatch(users, User.class));
        StepVerifier.create(newUserIdList)
                    .assertNext(list -> {
                        Assert.assertThat(list.size(), is(5));
                        Assert.assertThat(list.get(0), is(size + 1L));
                    })
                    .verifyComplete();
    }

    @Test
    public void testBatchInsertUsers2() {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = new User();
            user.setName("test insert batch " + i);
            user.setPassword("test insert pwd batch " + i);
            users.add(user);
        }
        Mono<List<Integer>> newUserIdList = exec(c -> c.insertObjectBatch(users, User.class,
                resultSet -> resultSet.stream().map(row -> row.getInt(1)).collect(Collectors.toList())));
        StepVerifier.create(newUserIdList)
                    .assertNext(list -> {
                        Assert.assertThat(list.size(), is(10));
                        Assert.assertThat(list.get(0), is(size + 1));
                    })
                    .verifyComplete();
    }

    @Test
    public void testDeleteUser() {
        Mono<Integer> row = exec(c -> c.deleteById(1L, User.class));
        StepVerifier.create(row).expectNext(1).verifyComplete();
    }

    @Test
    public void testUpdateUser() {
        User user = new User();
        user.setId(1L);
        user.setName("update user name");
        Mono<Integer> row = exec(c -> c.updateObject(user));
        StepVerifier.create(row).expectNext(1).verifyComplete();

        Mono<User> userMono = exec(c -> c.queryById(1, User.class));
        StepVerifier.create(userMono)
                    .assertNext(u -> Assert.assertThat(u.getName(), is("update user name")))
                    .verifyComplete();
    }

    @Test
    public void testQueryForSingleColumn() {
        String sql1 = "select count(*) from test.user";
        Mono<Long> count = exec(c -> c.queryForSingleColumn(sql1));
        StepVerifier.create(count).expectNext(Long.valueOf(size)).verifyComplete();

        String sql2 = "select count(*) from test.user where id > ?";
        count = exec(c -> c.queryForSingleColumn(sql2, 5L));
        StepVerifier.create(count).expectNext(size - 5L).verifyComplete();

        String namedSql = "select count(*) from test.user where id > :id";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("id", 5L);
        count = exec(c -> c.namedQueryForSingleColumn(namedSql, paramMap));
        StepVerifier.create(count).expectNext(size - 5L).verifyComplete();
    }

    @Test
    public void testQueryForObject() {
        String sql = "select * from test.user where id = ?";
        Mono<User> user = exec(c -> c.queryForObject(sql, User.class, 2L));
        StepVerifier.create(user)
                    .assertNext(u -> Assert.assertThat(u.getName(), is("test transaction 1")))
                    .verifyComplete();

        String namedSql = "select * from test.user where id = :id";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("id", 2L);
        user = exec(c -> c.namedQueryForObject(namedSql, User.class, paramMap));
        StepVerifier.create(user)
                    .assertNext(u -> Assert.assertThat(u.getName(), is("test transaction 1")))
                    .verifyComplete();
    }

    @Test
    public void testQueryForList() {
        String sql = "select * from test.user where id >= ?";
        Mono<List<User>> users = exec(c -> c.queryForList(sql, User.class, 9L));
        StepVerifier.create(users.flatMapIterable(tmpUsers -> tmpUsers).map(User::getName))
                    .expectNext("test transaction 8")
                    .expectNext("test transaction 9")
                    .verifyComplete();

        String namedSql = "select * from test.user where id >= :id";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("id", 9L);
        users = exec(c -> c.namedQueryForList(namedSql, User.class, paramMap));
        StepVerifier.create(users.flatMapIterable(tmpUsers -> tmpUsers).map(User::getName))
                    .expectNext("test transaction 8")
                    .expectNext("test transaction 9")
                    .verifyComplete();
    }

    @Test
    public void testQuery() {
        String sql = "select * from test.user where id >= ?";
        Mono<List<String>> userNames = exec(c ->
                c.query(sql, resultSet -> resultSet.stream()
                                                   .map(row -> row.getString("pt_name"))
                                                   .collect(Collectors.toList()), 9L));
        StepVerifier.create(userNames.flatMapIterable(tmpNames -> tmpNames))
                    .expectNext("test transaction 8")
                    .expectNext("test transaction 9")
                    .verifyComplete();

        String namedSql = "select * from test.user where id >= :id";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("id", 9L);
        userNames = exec(c ->
                c.namedQuery(namedSql, resultSet -> resultSet.stream()
                                                             .map(row -> row.getString("pt_name"))
                                                             .collect(Collectors.toList()), paramMap));
        StepVerifier.create(userNames.flatMapIterable(tmpNames -> tmpNames))
                    .expectNext("test transaction 8")
                    .expectNext("test transaction 9")
                    .verifyComplete();
    }

    @Test
    public void testQueryForMap() {
        String sql = "select * from test.user where id >= ?";
        Mono<Map<Long, User>> userMap = exec(c -> c.queryForBeanMap(sql,
                User.class, 9L));
        StepVerifier.create(userMap.map(map -> map.get(10L)).map(User::getName))
                    .expectNext("test transaction 9")
                    .verifyComplete();

        String namedSql = "select * from test.user where id >= :id";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("id", 9L);
        userMap = exec(c -> c.namedQueryForBeanMap(namedSql,
                User.class, paramMap));
        StepVerifier.create(userMap.map(map -> map.get(10L)).map(User::getName))
                    .expectNext("test transaction 9")
                    .verifyComplete();
    }

    @Test
    public void testInsert() {
        String sql = "insert into `test`.`user`(pt_name, pt_password) values(?,?)";
        Mono<Long> newUserId = exec(c -> c.insert(sql, "hello user", "hello user pwd"));
        StepVerifier.create(newUserId).expectNext(size + 1L).verifyComplete();

        String namedSql = "insert into `test`.`user`(pt_name, pt_password) values(:name, :password)";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", "hello user1");
        paramMap.put("password", "hello user pwd1");
        newUserId = exec(c -> c.namedInsert(namedSql, paramMap));
        StepVerifier.create(newUserId).expectNext(size + 2L).verifyComplete();

        User user = new User();
        user.setName("hello user2");
        user.setPassword("hello user pwd2");
        newUserId = exec(c -> c.namedInsert(namedSql, user));
        StepVerifier.create(newUserId).expectNext(size + 3L).verifyComplete();
    }

    @Test
    public void testUpdate() {
        String sql = "update test.user set `pt_name` = ? where id = ?";
        Mono<Integer> row = exec(c -> c.update(sql, "update xxx", 2L));
        StepVerifier.create(row).expectNext(1).verifyComplete();

        String namedSql = "update test.user set `pt_name` = :name where id = :id";
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", "update xxx");
        paramMap.put("id", 2L);
        row = exec(c -> c.namedUpdate(namedSql, paramMap));
        StepVerifier.create(row).expectNext(1).verifyComplete();

        User user = new User();
        user.setId(2L);
        user.setName("update xxx");
        row = exec(c -> c.namedUpdate(namedSql, user));
        StepVerifier.create(row).expectNext(1).verifyComplete();
    }
}
