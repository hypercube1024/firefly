package com.firefly.example.kotlin.coffee.store.vo

/**
 * @author Pengtao Qiu
 */
enum class OrderStatus(val value: Int,
                       val description: String) {
    UNPAID(1, "unpaid"),
    PURCHASED(2, "purchased"),
    SHIPPED(3, "shipped"),
    FINISHED(4, "finished"),
    CANCELED(5, "canceled"),
    REFUND(6, "refund")
}