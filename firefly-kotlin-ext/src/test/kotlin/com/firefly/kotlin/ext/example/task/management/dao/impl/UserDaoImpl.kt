package com.firefly.kotlin.ext.example.task.management.dao.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.kotlin.ext.db.AsyncTransactionalJDBCHelper
import com.firefly.kotlin.ext.example.task.management.dao.UserDao
import com.firefly.kotlin.ext.example.task.management.model.User

/**
 * @author Pengtao Qiu
 */
@Component
class UserDaoImpl : UserDao {

    @Inject
    lateinit var jdbcHelper: AsyncTransactionalJDBCHelper

    suspend override fun insert(user: User): Long? {
        return jdbcHelper.insertObject<User, Long>(user)
    }

    suspend override fun listUsers(userIdList: List<Long>): List<User> {
        val sql = "select * from test.user where id in (${userIdList.map { "?" }.joinToString(",")})"
        return jdbcHelper.queryForList<User>(sql, *userIdList.toTypedArray())
    }

}