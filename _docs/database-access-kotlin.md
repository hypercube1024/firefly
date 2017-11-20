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
	- [Insert data](#insert-data)
	- [Delete data](#delete-data)
	- [Update data](#update-data)
- [Query single column data](#query-single-column-data)
- [Prepared statement query](#prepared-statement-query)
- [Prepared statement update](#prepared-statement-update)

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

    init {
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
private suspend fun <T> exec(handler: suspend (conn: SQLConnection) -> T): T
            = sqlClient.connection.await().execSQL(handler)

@Before
fun before() = runBlocking {
    exec {
        it.asyncUpdate("DROP SCHEMA IF EXISTS test")
        it.asyncUpdate("CREATE SCHEMA IF NOT EXISTS test")
        it.asyncUpdate("set mode MySQL")
        val table = "CREATE TABLE IF NOT EXISTS `test`.`user`(" +
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
data class User(@Id("id") var id: Long?,
                @Column("pt_name") var name: String?,
                @Column("pt_password") var password: String?,
                var otherInfo: String?) {

    override fun equals(other: Any?): Boolean = if (other is User) Objects.equals(id, other.id) else false

    override fun hashCode(): Int = Objects.hashCode(id)
}
```

## Query data by id
```kotlin
@Test
fun testQueryById() = runBlocking {
    val user = exec {
        it.asyncQueryById<User>(1L)
    }
    assertEquals("test transaction 0", user.name)
}
```

In this example, the SQL client query test.user table by id, if the database has not the record, SQL client will emit a RecordNotFound exception. Such as:
```kotlin
@Test(expected = RecordNotFound::class)
fun testRecordNotFound() = runBlocking {
    exec {
        it.asyncQueryById<User>(size + 10)
    }
    Unit
}
```

## Insert data
We can insert a javabean into the database directly and the SQL client will return the autoincrement id. For example:
```kotlin
@Test
fun testInsertObject() = runBlocking {
    val newUserId = exec {
        val user = User(null, "test insert", "test insert pwd", null)
        it.asyncInsertObject<User, Long>(user)
    }
    assertEquals(size + 1L, newUserId)
}
```

Batch to insert data. For example:
```kotlin
@Test
fun testBatchInsertObject() = runBlocking {
    val newIdList = exec {
        it.asyncInsertObjectBatch<User, Long>(List(5) { index ->
            User(null, "test insert $index", "test insert pwd $index", null)
        })
    }
    assertEquals(5, newIdList.size)
    assertEquals(size + 1L, newIdList[0])
}
```

Batch to insert data and use the custom mapping. For example:
```kotlin
@Test
fun testBatchInsert2() = runBlocking {
    val newIdList = exec {
        it.asyncInsertObjectBatch<User, List<Int>>(List(5) { index ->
            User(null, "test insert $index", "test insert pwd $index", null)
        }, Func1 { rs -> rs.map { row -> row.getInt(1) } })
    }
    assertEquals(5, newIdList.size)
    assertEquals(size + 1, newIdList[0])
}
```

## Delete data
Delete user whose id is 1L and return affected row number. For example:
```kotlin
@Test
fun testDelete() = runBlocking {
    val rows = exec { it.asyncDeleteById<User>(1L) }
    assertEquals(1, rows)
}
```

## Update data
Update user whose id is 1L and return affected row number. For example:
```kotlin
@Test
fun testUpdateObject() = runBlocking {
    val rows = exec {
        it.asyncUpdateObject(User(1L, "update user name", null, null))
    }
    assertEquals(1, rows)

    val user = exec { it.asyncQueryById<User>(1L) }
    assertEquals("update user name", user.name)
}
```

# Query single column data
```kotlin
@Test
fun testQueryForSingleColumn() = runBlocking {
    val count = exec { it.asyncQueryForSingleColumn<Long>("select count(*) from test.user") }
    assertEquals(size.toLong(), count)

    val count2 = exec {
        it.asyncQueryForSingleColumn<Long>("select count(*) from test.user where id > ?", 5L)
    }
    assertEquals(size.toLong() - 5L, count2)

    val count3 = exec {
        it.asyncNamedQueryForSingleColumn<Long>(
                "select count(*) from test.user where id > :id",
                mapOf("id" to 5L))
    }
    assertEquals(size.toLong() - 5L, count3)
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
```kotlin
@Test
fun testQueryForObject() = runBlocking {
    val user = exec { it.asyncQueryForObject<User>("select * from test.user where id = ?", 2L) }
    assertEquals("test transaction 1", user.name)

    val user2 = exec {
        it.asyncNamedQueryForObject<User>("select * from test.user where id = :id",
                mapOf("id" to 2L))
    }
    assertEquals("test transaction 1", user2.name)
}
```

To execute a prepared statement query and return many rows. For example:
```kotlin
@Test
fun testQueryForList() = runBlocking {
    val users = exec { it.asyncQueryForList<User>("select * from test.user where id >= ?", 9L) }
    assertEquals("test transaction 8", users[0].name)
    assertEquals("test transaction 9", users[1].name)

    val users2 = exec {
        it.asyncNamedQueryForList<User>("select * from test.user where id >= :id", mapOf("id" to 9L))
    }
    assertEquals("test transaction 8", users2[0].name)
    assertEquals("test transaction 9", users2[1].name)
}
```

To execute a prepared statement query and use the custom mapping. For example:
```kotlin
@Test
    fun testQuery() = runBlocking {
        val names = exec {
            it.asyncQuery("select * from test.user where id >= ?", { rs ->
                rs.map { it.getString("pt_name") }
            }, 9L)
        }
        assertEquals("test transaction 8", names[0])
        assertEquals("test transaction 9", names[1])

        val names2 = exec {
            it.asyncNamedQuery("select * from test.user where id >= :id", { rs ->
                rs.map { it.getString("pt_name") }
            }, mapOf("id" to 9L))
        }
        assertEquals("test transaction 8", names2[0])
        assertEquals("test transaction 9", names2[1])
    }
```

To execute a prepared statement query and convert result to a map. For example:
```kotlin
@Test
fun testQueryForMap() = runBlocking {
    val userMap = exec {
        it.asyncQueryForBeanMap<Long, User>("select * from test.user where id >= ?", 9L)
    }
    assertEquals("test transaction 9", userMap[10L]?.name)

    val userMap2 = exec {
        it.asyncNamedQueryForBeanMap<Long, User>("select * from test.user where id >= :id", mapOf("id" to 9L))
    }
    assertEquals("test transaction 9", userMap2[10L]?.name)
}
```

# Prepared statement update
To execute a prepared statement insert. For example:
```kotlin
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
```kotlin
@Test
fun testUpdate() = runBlocking {
    val rows = exec {
        it.asyncUpdate("update test.user set `pt_name` = ? where id = ?", "update xxx", 2L)
    }
    assertEquals(rows, 1)

    val rows2 = exec {
        it.asyncNamedUpdate("update test.user set `pt_name` = :name where id = :id",
                mapOf("id" to 2L, "name" to "update xxx"))
    }
    assertEquals(rows2, 1)

    val rows3 = exec {
        it.asyncNamedUpdate("update test.user set `pt_name` = :name where id = :id",
                User(2L, "update xxx", null, null))
    }
    assertEquals(rows3, 1)
}
```
