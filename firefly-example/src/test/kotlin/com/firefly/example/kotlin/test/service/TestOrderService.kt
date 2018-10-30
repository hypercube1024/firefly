package com.firefly.example.kotlin.test.service

import com.firefly.example.kotlin.coffee.store.dao.InventoryDAO
import com.firefly.example.kotlin.coffee.store.ktCtx
import com.firefly.example.kotlin.coffee.store.model.Inventory
import com.firefly.example.kotlin.coffee.store.service.OrderService
import com.firefly.example.kotlin.coffee.store.service.UserService
import com.firefly.example.kotlin.coffee.store.vo.InventoryUpdate
import com.firefly.example.kotlin.coffee.store.vo.ProductBuyRequest
import com.firefly.example.kotlin.test.TestBase
import com.firefly.kotlin.ext.context.getBean
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.stream.Collectors
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestOrderService : TestBase() {

    private val orderService = ktCtx.getBean<OrderService>()
    private val userService = ktCtx.getBean<UserService>()
    private val inventoryDAO = ktCtx.getBean<InventoryDAO>()

    @Test
    fun test() = runBlocking {
        val user = userService.getByName("John")
        val req = ProductBuyRequest(
            user.id ?: 0L, listOf(
                InventoryUpdate(4L, 10L),
                InventoryUpdate(5L, 20L)
                                 )
                                   )
        orderService.buy(req)

        val inventories = inventoryDAO.listByProductId(listOf(4L, 5L))
        val map = inventories.stream().collect(Collectors.toMap(Inventory::productId) { v -> v })
        assertEquals(67L, map[4L]?.amount)
        assertEquals(80L, map[5L]?.amount)
    }

    @Test
    fun testError() = runBlocking {
        newTransaction {
            val user = userService.getByName("John")
            val req = ProductBuyRequest(
                user.id ?: 0L, listOf(
                    InventoryUpdate(4L, 90L),
                    InventoryUpdate(5L, 20L)
                                     )
                                       )
            orderService.buy(req)
        }.join()

        val inventories = inventoryDAO.listByProductId(listOf(4L, 5L))
        val map = inventories.stream().collect(Collectors.toMap(Inventory::productId) { v -> v })
        assertEquals(77L, map[4L]?.amount)
        assertEquals(100L, map[5L]?.amount)
    }

}