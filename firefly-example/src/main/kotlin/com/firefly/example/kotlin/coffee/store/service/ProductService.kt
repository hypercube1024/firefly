package com.firefly.example.kotlin.coffee.store.service

import com.firefly.example.kotlin.coffee.store.model.Product
import com.firefly.example.kotlin.coffee.store.vo.Page
import com.firefly.example.kotlin.coffee.store.vo.ProductQuery

/**
 * @author Pengtao Qiu
 */
interface ProductService {
    suspend fun list(query: ProductQuery): Page<Product>
}