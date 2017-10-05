package com.firefly.example.kotlin.coffee.store.service.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.example.kotlin.coffee.store.dao.UserDAO
import com.firefly.example.kotlin.coffee.store.model.User
import com.firefly.example.kotlin.coffee.store.service.UserService

/**
 * @author Pengtao Qiu
 */
@Component
class UserServiceImpl : UserService {

    @Inject
    lateinit var userDAO: UserDAO

    suspend override fun getByName(name: String): User = userDAO.getByName(name)

}