---

category : docs
layout: document
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

<!-- /TOC -->

# Create a client
The Firefly JDBC client allows you to interact with any JDBC compliant database using an asynchronous API. We provides two interfaces `ReactiveSQLClient` or `SQLClient`.

To use this project, add the following dependency to the dependencies section of your build descriptor:
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

In this case, we use the HikariDataSource and h2databse and create the ReactiveSQLClient instance. The ReactiveSQLClient wraps the SQLClient using spring reactor. The spring reactor provides more powerful asynchronous APIs than CompletableFuture.
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
                .expectComplete().verify();
}
```

In this case, the SQL client query test.user table by id, if the database has not the record, SQL client will emit a RecordNotFound exception. Such as:
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
                .expectComplete().verify();
}
```
