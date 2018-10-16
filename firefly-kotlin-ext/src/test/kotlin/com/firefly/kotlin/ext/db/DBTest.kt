package com.firefly.kotlin.ext.db

import com.firefly.db.RecordNotFound
import com.firefly.db.SQLClient
import com.firefly.db.SQLConnection
import com.firefly.db.annotation.Column
import com.firefly.db.annotation.Id
import com.firefly.db.annotation.Table
import com.firefly.db.jdbc.JDBCClient
import com.firefly.utils.function.Func1
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class DBTest {

    private val sqlClient: SQLClient
    private val size = 10

    init {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:h2:mem:test"
        config.driverClassName = "org.h2.Driver"
        config.isAutoCommit = false
        val ds = HikariDataSource(config)
        sqlClient = JDBCClient(ds)
    }

    private suspend fun <T> exec(handler: suspend (conn: SQLConnection) -> T): T =
        sqlClient.connection.await().execSQL(10000, TimeUnit.MILLISECONDS, handler)

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
                params[i][0] = "test transaction $i"
                params[i][1] = "pwd transaction $i"
            }
            val sql = "insert into `test`.`user`(pt_name, pt_password) values(?,?)"
            val id = it.insertBatch(sql, params) { rs ->
                rs.stream().map { r -> r.getInt(1) }.collect(Collectors.toList<Any>())
            }.await()
            println(id)
        }
    }

    @After
    fun after() = runBlocking {
        exec {
            it.asyncUpdate("DROP TABLE IF EXISTS `test`.`user`")
        }
        Unit
    }

    @Test
    fun testQueryById() = runBlocking {
        val user = exec {
            it.asyncQueryById<User>(1L)
        }
        assertEquals("test transaction 0", user.name)
    }

    @Test(expected = RecordNotFound::class)
    fun testRecordNotFound() = runBlocking {
        exec {
            it.asyncQueryById<User>(size + 10)
        }
        Unit
    }

    @Test
    fun testInsertObject() = runBlocking {
        val newUserId = exec {
            val user = User(null, "test insert", "test insert pwd", null)
            it.asyncInsertObject<User, Long>(user)
        }
        assertEquals(size + 1L, newUserId)
    }

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

    @Test
    fun testDelete() = runBlocking {
        val rows = exec { it.asyncDeleteById<User>(1L) }
        assertEquals(1, rows)
    }

    @Test
    fun testUpdateObject() = runBlocking {
        val rows = exec {
            it.asyncUpdateObject(User(1L, "update user name", null, null))
        }
        assertEquals(1, rows)

        val user = exec { it.asyncQueryById<User>(1L) }
        assertEquals("update user name", user.name)
    }

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
                mapOf("id" to 5L)
                                                   )
        }
        assertEquals(size.toLong() - 5L, count3)
    }

    @Test
    fun testQueryForObject() = runBlocking {
        val user = exec { it.asyncQueryForObject<User>("select * from test.user where id = ?", 2L) }
        assertEquals("test transaction 1", user.name)

        val user2 = exec {
            it.asyncNamedQueryForObject<User>(
                "select * from test.user where id = :id",
                mapOf("id" to 2L)
                                             )
        }
        assertEquals("test transaction 1", user2.name)
    }

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

    @Test
    fun testInsert() = runBlocking {
        val newUserId = exec {
            it.asyncInsert<Long>(
                "insert into `test`.`user`(pt_name, pt_password) values(?,?)",
                "hello user", "hello user pwd"
                                )
        }
        assertEquals(size + 1L, newUserId)

        val namedSQL = "insert into `test`.`user`(pt_name, pt_password) values(:name, :password)"
        val newUserId2 = exec {
            it.asyncNamedInsert<Long>(namedSQL, mapOf("name" to "hello user", "password" to "hello user pwd"))
        }
        assertEquals(size + 2L, newUserId2)

        val newUserId3 = exec {
            it.asyncNamedInsert<Long>(namedSQL, User(null, "hello user", "hello user pwd", null))
        }
        assertEquals(size + 3L, newUserId3)
    }

    @Test
    fun testUpdate() = runBlocking {
        val rows = exec {
            it.asyncUpdate("update test.user set `pt_name` = ? where id = ?", "update xxx", 2L)
        }
        assertEquals(rows, 1)

        val rows2 = exec {
            it.asyncNamedUpdate(
                "update test.user set `pt_name` = :name where id = :id",
                mapOf("id" to 2L, "name" to "update xxx")
                               )
        }
        assertEquals(rows2, 1)

        val rows3 = exec {
            it.asyncNamedUpdate(
                "update test.user set `pt_name` = :name where id = :id",
                User(2L, "update xxx", null, null)
                               )
        }
        assertEquals(rows3, 1)
    }

    @Table(value = "user", catalog = "test")
    data class User(
        @Id("id") var id: Long?,
        @Column("pt_name") var name: String?,
        @Column("pt_password") var password: String?,
        var otherInfo: String?
                   ) {

        override fun equals(other: Any?): Boolean = if (other is User) Objects.equals(id, other.id) else false

        override fun hashCode(): Int = Objects.hashCode(id)
    }
}