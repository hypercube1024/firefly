package com.firefly.example.kotlin.coffee.store.service.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.example.kotlin.coffee.store.dao.InventoryDAO
import com.firefly.example.kotlin.coffee.store.dao.OrderDAO
import com.firefly.example.kotlin.coffee.store.dao.ProductDAO
import com.firefly.example.kotlin.coffee.store.model.Order
import com.firefly.example.kotlin.coffee.store.model.Product
import com.firefly.example.kotlin.coffee.store.service.OrderService
import com.firefly.example.kotlin.coffee.store.vo.InventoryOperator
import com.firefly.example.kotlin.coffee.store.vo.OrderStatus
import com.firefly.example.kotlin.coffee.store.vo.ProductBuyRequest
import com.firefly.utils.CollectionUtils
import java.math.BigDecimal
import java.util.*
import java.util.stream.Collectors

/**
 * @author Pengtao Qiu
 */
@Component
class OrderServiceImpl : OrderService {

    @Inject
    private lateinit var orderDAO: OrderDAO

    @Inject
    private lateinit var inventoryDAO: InventoryDAO

    @Inject
    private lateinit var productDAO: ProductDAO

    override suspend fun buy(request: ProductBuyRequest) {
        if (Objects.equals(request.userId, 0L)) {
            throw IllegalArgumentException("The user id is required")
        }

        if (CollectionUtils.isEmpty(request.products)) {
            throw IllegalArgumentException("The products must bu not empty")
        }

        verifyInventory(inventoryDAO.updateBatch(request.products, InventoryOperator.SUB))
        val products = productDAO.list(toProductIdList(request))
        val orders = toOrders(request, products)
        orderDAO.insertBatch(orders)
    }

    private fun verifyInventory(arr: IntArray) {
        if (Arrays.stream(arr).anyMatch { i -> i <= 0 }) {
            throw IllegalStateException("The products are not enough")
        }
    }

    private fun toProductIdList(request: ProductBuyRequest): List<Long> {
        return request.products.parallelStream().map { it.productId }.collect(Collectors.toList<Long>())
    }

    private fun toOrders(request: ProductBuyRequest, products: List<Product>): List<Order> {
        return products.parallelStream().map { product ->
            val amount = getAmount(request, product)
            val totalPrice = BigDecimal.valueOf(product.price).multiply(BigDecimal.valueOf(amount)).toDouble()

            Order(null, OrderStatus.FINISHED.value,
                    amount, product.price, totalPrice,
                    product.id ?: 0,
                    request.userId ?: 0L,
                    product.description,
                    null, null)
        }.collect(Collectors.toList<Order>())
    }

    private fun getAmount(request: ProductBuyRequest, product: Product): Long {
        return request.products.parallelStream()
                .filter { i -> Objects.equals(i.productId, product.id) }
                .map { it.amount }
                .findFirst()
                .orElseThrow({ IllegalStateException("The product amounts must be more than 0") })
    }
}