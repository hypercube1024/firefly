package com.firefly.example.kotlin.coffee.store.service

import com.firefly.example.kotlin.coffee.store.model.User

/**
 * @author Pengtao Qiu
 */
interface UserService {
    suspend fun getByName(name: String): User
}