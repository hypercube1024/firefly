package com.firefly.kotlin.ext.example.task.management.dao

import com.firefly.kotlin.ext.example.task.management.model.Task

/**
 * @author Pengtao Qiu
 */
interface TaskDao {

    suspend fun insert(task: Task): Long?

    suspend fun listTasksByUserId(userId: Long): List<Task>

}