package com.firefly.kotlin.ext.example.task.management.service.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.kotlin.ext.example.task.management.dao.UserDao
import com.firefly.kotlin.ext.example.task.management.model.User
import com.firefly.kotlin.ext.example.task.management.service.UserService
import com.firefly.kotlin.ext.example.task.management.vo.Request
import com.firefly.kotlin.ext.example.task.management.vo.Response

/**
 * @author Pengtao Qiu
 */
@Component
class UserServiceImpl : UserService {

    @Inject
    lateinit var userDao: UserDao

    override suspend fun getUser(request: Request<Long>): Response<User> {
        val list = userDao.listUsers(listOf(request.data))
        return Response(0, "success", if (list.isEmpty()) null else list[0])
    }

    override suspend fun listUsers(request: Request<List<Long>>): Response<List<User>> {
        return Response(0, "success", userDao.listUsers(request.data))
    }

    override suspend fun insert(request: Request<User>): Response<Long> {
        return Response(0, "success", userDao.insert(request.data))
    }

}