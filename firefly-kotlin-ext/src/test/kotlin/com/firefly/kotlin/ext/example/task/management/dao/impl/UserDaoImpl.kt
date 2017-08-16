package com.firefly.kotlin.ext.example.task.management.dao.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.kotlin.ext.db.AsyncTransactionalManager
import com.firefly.kotlin.ext.db.asyncInsertObject
import com.firefly.kotlin.ext.db.asyncQueryForList
import com.firefly.kotlin.ext.example.task.management.dao.UserDao
import com.firefly.kotlin.ext.example.task.management.model.User

/**
 * @author Pengtao Qiu
 */
@Component
class UserDaoImpl : UserDao {

    @Inject
    lateinit var dbClient: AsyncTransactionalManager

    suspend override fun insert(user: User): Long? = dbClient.execSQL {
        it.asyncInsertObject<User, Long>(user)
    }

    suspend override fun listUsers(userIdList: List<Long>): List<User> = dbClient.execSQL {
        val sql = "select * from test.user where id in (${userIdList.map { "?" }.joinToString(",")})"
        it.asyncQueryForList<User>(sql, *userIdList.toTypedArray())
    } ?: listOf()

}