package com.firefly.example.kotlin.test.dao

import com.firefly.example.kotlin.coffee.store.dao.UserDAO
import com.firefly.example.kotlin.test.TestBase
import com.firefly.kotlin.ext.context.Context
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestUserDAO : TestBase() {

    private val userDAO = Context.getBean<UserDAO>()

    @Test
    fun test() = runBlocking {
        val user = userDAO.getByName("John")
        assertEquals(2L, user.id)
        assertEquals("123456", user.password)
    }
}