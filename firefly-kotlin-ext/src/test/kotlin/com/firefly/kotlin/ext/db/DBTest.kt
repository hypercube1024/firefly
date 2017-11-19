package com.firefly.kotlin.ext.db

import com.firefly.db.RecordNotFound
import com.firefly.db.SQLClient
import com.firefly.db.annotation.Column
import com.firefly.db.annotation.Id
import com.firefly.db.annotation.Table
import com.firefly.db.jdbc.JDBCClient
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

    constructor() {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:h2:mem:test"
        config.driverClassName = "org.h2.Driver"
        config.isAutoCommit = false
        val ds = HikariDataSource(config)
        sqlClient = JDBCClient(ds)
    }

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

    @After
    fun after() = runBlocking {
        sqlClient.connection.await().execSQL {
            it.asyncUpdate("DROP TABLE IF EXISTS `test`.`user`")
        }
        Unit
    }

    @Test
    fun testQueryById() = runBlocking {
        val user = sqlClient.connection.await().execSQL {
            it.asyncQueryById<User>(1L)
        }
        assertEquals("test transaction 0", user.name)
    }

    @Test(expected = RecordNotFound::class)
    fun testRecordNotFound() = runBlocking {
        sqlClient.connection.await().execSQL {
            it.asyncQueryById<User>(size + 10)
        }
        Unit
    }


    @Table(value = "user", catalog = "test")
    data class User(@Id("id") var id: Long,
                    @Column("pt_name") var name: String,
                    @Column("pt_password") var password: String,
                    var otherInfo: String) {

        override fun equals(other: Any?): Boolean = if (other is User) Objects.equals(id, other.id) else false

        override fun hashCode(): Int = Objects.hashCode(id)
    }
}