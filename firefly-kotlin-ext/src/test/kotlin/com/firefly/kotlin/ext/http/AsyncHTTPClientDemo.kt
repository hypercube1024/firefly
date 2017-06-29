package com.firefly.kotlin.ext.http

import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.log.Log
import kotlinx.coroutines.experimental.runBlocking

/**
 * HTTP asynchronous wait example
 *
 * @author Pengtao Qiu
 */

private val log = Log.getLogger { }

private val host = "http://localhost:8080"

fun main(args: Array<String>): Unit = runBlocking {
    val msg = firefly.httpClient().get("$host/jsonMsg")
            .asyncSubmit().getJsonBody<Response<String>>()
    log.info("msg -> $msg")

    val product = firefly.httpClient().get("$host/product/fuck/3")
            .asyncSubmit().getJsonBody<Response<Product>>()
    log.info("product -> $product")
}