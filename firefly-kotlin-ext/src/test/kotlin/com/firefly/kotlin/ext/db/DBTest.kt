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

    private suspend fun <T> exec(handler: suspend (conn: SQLConnection) -> T): T
            = sqlClient.connection.await().execSQL(handler)

    @Before
    fun before() = runBlocking {
        exec {
            it.asyncUpdate("CREATE SCHEMA IF NOT EXISTS test")
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
    fun testInsert() = runBlocking {
        val newUserId = exec {
            val user = User(null, "test insert", "test insert pwd", null)
            it.asyncInsertObject<User, Long>(user)
        }
        assertEquals(size + 1L, newUserId)
    }

    @Test
    fun testBatchInsert() = runBlocking {
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
    fun testUpdate() = runBlocking {
        val rows = exec {
            it.asyncUpdateObject(User(1L, "update user name", null, null))
        }
        assertEquals(1, rows)

        val user = exec { it.asyncQueryById<User>(1L) }
        assertEquals("update user name", user.name)
    }


    @Table(value = "user", catalog = "test")
    data class User(@Id("id") var id: Long?,
                    @Column("pt_name") var name: String?,
                    @Column("pt_password") var password: String?,
                    var otherInfo: String?) {

        override fun equals(other: Any?): Boolean = if (other is User) Objects.equals(id, other.id) else false

        override fun hashCode(): Int = Objects.hashCode(id)
    }
}