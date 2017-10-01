package com.firefly.example.kotlin.coffee.store.vo

import com.firefly.kotlin.ext.annotation.NoArg

/**
 * @author Pengtao Qiu
 */
@NoArg
data class ProductQuery(var status: Int? = null,
                        var type: Int? = null,
                        var searchKey: String? = null,
                        var pageNumber: Int = 1,
                        var pageSize: Int = 20)