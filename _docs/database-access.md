---

category : docs
title: Database access

---

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Create a client](#create-a-client)
- [Execute transaction](#execute-transaction)
- [Bind data to java bean](#bind-data-to-java-bean)
	- [Create mapping](#create-mapping)
	- [Query data by id](#query-data-by-id)
	- [Insert data](#insert-data)
	- [Delete data](#delete-data)
	- [Update data](#update-data)
- [Query single column data](#query-single-column-data)
- [Prepared statement query](#prepared-statement-query)
- [Prepared statement update](#prepared-statement-update)

<!-- /TOC -->

# Create a client
The Firefly JDBC client allows you to interact with any JDBC compliant database using an asynchronous API. We provides two interfaces `ReactiveSQLClient` or `SQLClient`.

Add the following dependency to the dependencies section of your build descriptor:
```xml
<dependency>
    <groupId>com.fireflysource</groupId>
    <artifactId>firefly-db</artifactId>
    <version>{{ site.data.global.releaseVersion }}</version>
</dependency>

<dependency>
    <groupId>com.fireflysource</groupId>
    <artifactId>firefly-reactive</artifactId>
    <version>{{ site.data.global.releaseVersion }}</version>
</dependency>

<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>2.7.2</version>
</dependency>

<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>1.4.196</version>
</dependency>
```

In this example, we use the HikariDataSource and h2databse and create the ReactiveSQLClient instance. The ReactiveSQLClient wraps the SQLClient using spring reactor. The spring reactor provides more powerful asynchronous APIs than CompletableFuture.
```java
public class TestReactiveSQLClient {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private ReactiveSQLClient sqlClient;

    public TestReactiveSQLClient() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test");
        config.setDriverClassName("org.h2.Driver");
        config.setAutoCommit(false);
        HikariDataSource ds = new HikariDataSource(config);
        sqlClient = Reactor.db.fromSQLClient(new JDBCClient(ds));
    }
}
```

# Execute transaction
We use `newTransaction` method to create a new transaction. The transaction will submit or rollback automatically when the callback is complete.
```java
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
```

# Bind data to java bean
We use the annotation to bind database result set to a java bean. Such as, `com.firefly.db.annotation.Table`, `com.firefly.db.annotation.Column`, and `com.firefly.db.annotation.Id`.

## Create mapping
Creating User class and mapping it to table test.user.
* `@Table` - set the database name and table name.
* `@Column`- set the table column name.
* `@Id` - set the primary key of the table.

```java
@Table(value = "user", catalog = "test")
public class User {
	@Id("id")
	private Long id;
	@Column("pt_name")
	private String name;
	private String password;
	private String otherInfo;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column("pt_password")
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getOtherInfo() {
		return otherInfo;
	}

	public void setOtherInfo(String otherInfo) {
		this.otherInfo = otherInfo;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", name=" + name + ", password=" + password + ", otherInfo=" + otherInfo + "]";
	}

}
```

## Query data by id
```java
@Test
public void testQueryById() {
    Mono<User> user = exec(c -> c.queryById(1, User.class));
    StepVerifier.create(user)
                .assertNext(u -> Assert.assertThat(u.getName(), is("test transaction 0")))
                .verifyComplete();
}
```

In this example, the SQL client query test.user table by id, if the database has not the record, SQL client will emit a RecordNotFound exception. Such as:
```java
@Test
public void testRecordNotFound() {
    Mono<User> user = exec(c -> c.queryById(size + 10, User.class));
    StepVerifier.create(user)
                .expectErrorMatches(t -> t.getCause() instanceof RecordNotFound)
                .verify();
}
```


## Insert data
We can insert a javabean into the database directly and the SQL client will return the autoincrement id. For example:
```java
@Test
public void testInsertUser() {
    User user = new User();
    user.setName("test insert");
    user.setPassword("test insert pwd");
    Mono<Long> newUserId = exec(c -> c.insertObject(user));
    StepVerifier.create(newUserId).expectNext(size + 1L).verifyComplete();
}
```

Batch to insert data. For example:
```java
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
```

Batch to insert data and use the custom mapping. For example:
```java
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
```

## Delete data
Delete user whose id is 1L and return affected row number. For example:
```java
@Test
public void testDeleteUser() {
    Mono<Integer> row = exec(c -> c.deleteById(1L, User.class));
    StepVerifier.create(row).expectNext(1).verifyComplete();
}
```

## Update data
Update user whose id is 1L and return affected row number. For example:
```java
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
```

# Query single column data
```java
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
```
Notes: If the return type does not equals the column type, it will throw cast class exception. The named SQL placeholder can start with ':' or '&'. For example:
```
select count(*) from test.user where id > :id
select count(*) from test.user where id > &id
```
You can also use the `:{xxx}` placeholder. Such as:
```
select count(*) from test.user where id > :{id}
```

# Prepared statement query
To execute a prepared statement query and return one row. For example:
```java
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
```

To execute a prepared statement query and return many rows. For example:
```java
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
```

To execute a prepared statement query and use the custom mapping. For example:
```java
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
```

To execute a prepared statement query and convert result to a map. For example:
```java
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
```

# Prepared statement update
To execute a prepared statement insert. For example:
```java
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
```
When we use the named SQL, we can use javabean or map to replace the placeholders. The javabean uses the property name to match the parameter. The map uses the key to match the parameter.

To execute a prepared statement update. For example:
```java
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
```
