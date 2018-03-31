package com.firefly.example.kotlin.coffee.store.dao.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.example.kotlin.coffee.store.dao.UserDAO
import com.firefly.example.kotlin.coffee.store.model.User
import com.firefly.kotlin.ext.db.AsyncTransactionalManager
import com.firefly.kotlin.ext.db.asyncQueryForObject
import com.firefly.utils.StringUtils

/**
 * @author Pengtao Qiu
 */
@Component
class UserDAOImpl : UserDAO {

    @Inject
    private lateinit var db: AsyncTransactionalManager

    override suspend fun getByName(name: String): User {
        if (!StringUtils.hasText(name)) {
            throw IllegalArgumentException("The username is required")
        }

        val sql = "select * from `coffee_store`.`user` where `name` = ?"
        return db.execSQL { it.asyncQueryForObject<User>(sql, name) }
    }

}