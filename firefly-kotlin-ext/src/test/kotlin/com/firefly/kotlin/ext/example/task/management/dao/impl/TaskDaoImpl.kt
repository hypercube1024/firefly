package com.firefly.kotlin.ext.example.task.management.dao.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.kotlin.ext.db.AsyncTransactionalJDBCHelper
import com.firefly.kotlin.ext.example.task.management.dao.TaskDao
import com.firefly.kotlin.ext.example.task.management.model.Task

/**
 * @author Pengtao Qiu
 */
@Component
class TaskDaoImpl : TaskDao {

    @Inject
    lateinit var jdbcHelper: AsyncTransactionalJDBCHelper

    suspend override fun insert(task: Task): Long? = jdbcHelper.insertObject<Task, Long>(task)

    suspend override fun listTasksByUserId(userId: Long): List<Task> {
        return jdbcHelper.queryForList<Task>("select * from test.task where user_id = ?", userId)
    }

}