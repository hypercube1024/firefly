package com.firefly.example.kotlin.coffee.store.vo

import com.firefly.kotlin.ext.annotation.NoArg

/**
 * @author Pengtao Qiu
 */
@NoArg
data class Response<T>(
    var status: Int,
    var message: String,
    var data: T
                      )

enum class ResponseStatus(
    val value: Int,
    val description: String
                         ) {
    OK(1, "ok"),
    ARGUMENT_ERROR(2, "argument error"),
    SERVER_ERROR(3, "server error")
}