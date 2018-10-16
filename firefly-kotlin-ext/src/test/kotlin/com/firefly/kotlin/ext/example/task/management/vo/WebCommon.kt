package com.firefly.kotlin.ext.example.task.management.vo

import com.firefly.kotlin.ext.annotation.NoArg

/**
 * @author Pengtao Qiu
 */
@NoArg
data class Request<T>(
    var authToken: String,
    var data: T
                     )

@NoArg
data class Response<T>(
    var code: Int,
    var message: String,
    var data: T?
                      )