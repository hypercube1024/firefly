package com.firefly.kotlin.ext.example

import com.firefly.kotlin.ext.annotation.NoArg

/**
 * @author Pengtao Qiu
 */
@NoArg
data class Response<T>(var msg: String, var code: Int, var data: T? = null)

@NoArg
data class Request<T>(var token: String, var data: T? = null)