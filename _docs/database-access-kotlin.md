---

category : docs
layout: document
title: Database access Kotlin version

---

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Create a client](#create-a-client)
- [Execute transaction](#execute-transaction)
- [Bind data to java bean](#bind-data-to-java-bean)
	- [Create mapping](#create-mapping)
	- [Query data by id](#query-data-by-id)

<!-- /TOC -->

# Create a client
The Firefly JDBC client allows you to interact with any JDBC compliant database using an asynchronous API. We use the Kotlin coroutine to convert the Firefly JDBC client to the synchronous blocking code style, but it does not block current thread.

Add the following dependency to the dependencies section of your build descriptor:
```xml
<dependency>
    <groupId>com.fireflysource</groupId>
    <artifactId>firefly-kotlin-ext</artifactId>
    <version>{{ site.data.global.releaseVersion }}</version>
</dependency>
```

In this example, we use the HikariDataSource and h2databse and create the SQLClient instance.
```kotlin
class DBTest {

    private val sqlClient: SQLClient

    constructor() {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:h2:mem:test"
        config.driverClassName = "org.h2.Driver"
        config.isAutoCommit = false
        val ds = HikariDataSource(config)
        sqlClient = JDBCClient(ds)
    }

}
```

# Execute transaction
We use `execSQL` method to create a new transaction. The transaction will submit or rollback automatically when the callback is complete.
```kotlin
@Before
fun before() = runBlocking {
    sqlClient.connection.await().execSQL {
        it.asyncUpdate("create schema test")
        it.asyncUpdate("set mode MySQL")
        val table = "CREATE TABLE `test`.`user`(" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "pt_name VARCHAR(255), " +
                "pt_password VARCHAR(255), " +
                "other_info VARCHAR(255))"
        it.asyncUpdate(table)
        val params = Array<Array<Any>>(size) { Array(2) { } }
        for (i in 0 until size) {
            params[i][0] = "test transaction " + i
            params[i][1] = "pwd transaction " + i
        }
        val sql = "insert into `test`.`user`(pt_name, pt_password) values(?,?)"
        val id = it.insertBatch(sql, params, { rs ->
            rs.stream().map { r -> r.getInt(1) }.collect(Collectors.toList<Any>())
        }).await()
        println(id)
    }
}
```

# Bind data to java bean
We use the annotation to bind database result set to a java bean. Such as, `com.firefly.db.annotation.Table`, `com.firefly.db.annotation.Column`, and `com.firefly.db.annotation.Id`.

## Create mapping
Creating User class and mapping it to table test.user.
* `@Table` - set the database name and table name.
* `@Column`- set the table column name.
* `@Id` - set the primary key of the table.

```kotlin
@Table(value = "user", catalog = "test")
data class User(@Id("id") var id: Long,
                @Column("pt_name") var name: String,
                @Column("pt_password") var password: String,
                var otherInfo: String) {

    override fun equals(other: Any?): Boolean = if (other is User) Objects.equals(id, other.id) else false

    override fun hashCode(): Int = Objects.hashCode(id)
}
```

## Query data by id
```kotlin
@Test
fun testQueryById() = runBlocking {
    val user = sqlClient.connection.await().execSQL {
        it.asyncQueryById<User>(1L)
    }
    assertEquals("test transaction 0", user.name)
}
```

In this example, the SQL client query test.user table by id, if the database has not the record, SQL client will emit a RecordNotFound exception. Such as:
```kotlin
@Test(expected = RecordNotFound::class)
fun testRecordNotFound() = runBlocking {
    sqlClient.connection.await().execSQL {
        it.asyncQueryById<User>(size + 10)
    }
    Unit
}
```
