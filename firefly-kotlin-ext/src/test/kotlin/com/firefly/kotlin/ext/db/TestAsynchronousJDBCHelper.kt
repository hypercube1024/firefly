package com.firefly.kotlin.ext.db

import com.firefly.db.annotation.Column
import com.firefly.db.annotation.Id
import com.firefly.db.annotation.Table
import com.firefly.kotlin.ext.context.Context
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestAsynchronousJDBCHelper {
    private val size = 20

    @Before
    fun setup() = runBlocking {
        val jdbcHelper = Context.getBean<AsynchronousJDBCHelper>()
        jdbcHelper.update("drop schema if exists test")
        jdbcHelper.update("create schema test")
        jdbcHelper.update("set mode MySQL")

        val createTable = """
            CREATE TABLE `test`.`project` (
            `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
            `name` VARCHAR(64) NOT NULL,
            `comment` VARCHAR(255),
            `members` VARCHAR(255),
            `status` INT NOT NULL
            )"""
        jdbcHelper.update(createTable)

        val list = (1..size).mapTo(mutableListOf<Project>()) { Project(null, "project_$it", "comment_$it", "a,b,c", 1) }
        val idList = jdbcHelper.insertObjectBatch<Project, Long>(list)
        println("id list -> $idList")
    }

    @Test
    fun test() = runBlocking {
        val jdbcHelper = Context.getBean<AsynchronousJDBCHelper>()
        for (i in 1L..size) {
            val project = jdbcHelper.queryById<Project>(i)
            assertEquals(i, project?.id)
            assertEquals("project_$i", project?.name)
            assertEquals("comment_$i", project?.comment)
        }
    }

    @Test
    fun testRollback() = runBlocking {
        val id = 1L

        asyncTransaction {
            val project = it.queryById<Project>(id)
            project?.members = "e,f,g"
            val rows = it.updateObject(project)
            assertEquals(1, rows)

            val updatedProject = it.queryById<Project>(id)
            assertEquals("e,f,g", updatedProject?.members)
            it.transactionalManager.rollback()
        }

        val project = Context.getBean<AsynchronousJDBCHelper>().queryById<Project>(id)
        assertEquals("a,b,c", project?.members)
    }
}

@Table(value = "project", catalog = "test")
data class Project(@Id("id") var id: Long?,
                   @Column("name") var name: String,
                   @Column("comment") var comment: String,
                   @Column("members") var members: String,
                   @Column("status") var status: Int) {

    override fun equals(other: Any?): Boolean {
        return if (other is Project) Objects.equals(id, other.id) else false
    }

    override fun hashCode(): Int {
        return Objects.hashCode(id)
    }
}