package com.firefly.example.kotlin.coffee.store.dao

import com.firefly.example.reactive.coffee.store.model.User

/**
 * @author Pengtao Qiu
 */
interface UserDAO {

    suspend fun getByName(name: String): User

}