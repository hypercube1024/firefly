package com.firefly.example.kotlin.test.dao

import com.firefly.db.RecordNotFound
import com.firefly.example.kotlin.coffee.store.dao.ProductDAO
import com.firefly.example.kotlin.coffee.store.vo.ProductQuery
import com.firefly.example.kotlin.coffee.store.vo.ProductStatus
import com.firefly.example.kotlin.coffee.store.vo.ProductType
import com.firefly.example.kotlin.test.TestBase
import com.firefly.kotlin.ext.context.Context
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun testList() = runBlocking {
        val page = productDAO.list(ProductQuery(null, ProductStatus.ENABLE.value, null, 1, 5))
        assertEquals(5, page.getRecord().size)

        val page2 = productDAO.list(ProductQuery("Co", ProductStatus.ENABLE.value, null, 1, 5))
        assertEquals(2, page2.getRecord().size)

        val page3 = productDAO.list(ProductQuery(null, ProductStatus.ENABLE.value, null, 2, 5))
        assertFalse { page3.isNext }
        assertTrue { page3.getRecord().size <= 5 }

        val page4 = productDAO.list(ProductQuery(null, ProductStatus.ENABLE.value, ProductType.DESSERT.value, 1, 5))
        assertEquals(1, page4.getRecord().size)
    }
}