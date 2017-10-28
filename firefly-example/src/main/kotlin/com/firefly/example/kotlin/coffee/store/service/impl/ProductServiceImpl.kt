package com.firefly.example.kotlin.coffee.store.service.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.example.kotlin.coffee.store.dao.ProductDAO
import com.firefly.example.kotlin.coffee.store.model.Product
import com.firefly.example.kotlin.coffee.store.service.ProductService
import com.firefly.example.kotlin.coffee.store.vo.Page
import com.firefly.example.kotlin.coffee.store.vo.ProductQuery

/**
 * @author Pengtao Qiu
 */
@Component
class ProductServiceImpl : ProductService {

    @Inject
    private lateinit var productDAO: ProductDAO

    suspend override fun list(query: ProductQuery): Page<Product> = productDAO.list(query)

}