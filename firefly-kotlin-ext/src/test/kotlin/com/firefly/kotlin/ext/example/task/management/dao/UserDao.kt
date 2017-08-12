package com.firefly.kotlin.ext.example.task.management.dao

import com.firefly.kotlin.ext.example.task.management.model.User

/**
 * @author Pengtao Qiu
 */
interface UserDao {

    suspend fun insert(user: User): Long?

    suspend fun listUsers(userIdList: List<Long>): List<User>

}