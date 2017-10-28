package com.firefly.example.kotlin.coffee.store.service

import com.firefly.example.kotlin.coffee.store.vo.ProductBuyRequest

/**
 * @author Pengtao Qiu
 */
interface OrderService {
    suspend fun buy(request: ProductBuyRequest)
}