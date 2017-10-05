package com.firefly.example.kotlin.test.dao

import com.firefly.db.RecordNotFound
import com.firefly.example.kotlin.coffee.store.dao.ProductDAO
import com.firefly.example.kotlin.test.TestBase
import com.firefly.kotlin.ext.context.Context
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestProductDAO : TestBase() {

    private val productDAO = Context.getBean<ProductDAO>()

    @Test
    fun testGet() = runBlocking {
        val product = productDAO.get(1L)
        assertEquals(1L, product.id)
        assertEquals("Cappuccino", product.name)
    }

    @Test(expected = RecordNotFound::class)
    fun testGetException(): Unit = runBlocking {
        productDAO.get(200L)
        Unit
    }
}