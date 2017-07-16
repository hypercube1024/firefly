package com.firefly.kotlin.ext.example.task.management.service

import com.firefly.kotlin.ext.example.task.management.model.User
import com.firefly.kotlin.ext.example.task.management.vo.Request
import com.firefly.kotlin.ext.example.task.management.vo.Response

/**
 * @author Pengtao Qiu
 */
interface UserService {

    suspend fun listUsers(request: Request<List<Long>>): Response<List<User>>

}