package com.firefly.kotlin.ext.example.task.management.dao.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.kotlin.ext.db.AsyncTransactionalManager
import com.firefly.kotlin.ext.db.asyncInsertObject
import com.firefly.kotlin.ext.db.asyncQueryForList
import com.firefly.kotlin.ext.db.execSQL
import com.firefly.kotlin.ext.example.task.management.dao.TaskDao
import com.firefly.kotlin.ext.example.task.management.model.Task

/**
 * @author Pengtao Qiu
 */
@Component
class TaskDaoImpl : TaskDao {

    @Inject
    lateinit var dbClient: AsyncTransactionalManager

    suspend override fun insert(task: Task): Long? = dbClient.execSQL {
        it.asyncInsertObject<Task, Long>(task)
    }

    suspend override fun listTasksByUserId(userId: Long): List<Task> = dbClient.execSQL {
        it.asyncQueryForList<Task>("select * from test.task where user_id = ?", userId)
    } ?: listOf()

}