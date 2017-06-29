package com.firefly.kotlin.ext.http

import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.log.Log

/**
 * HTTP asynchronous response example
 *
 * @author Pengtao Qiu
 */

private val log = Log.getLogger { }

fun main(args: Array<String>) {
    firefly.httpServer().router().get("/product/:type/:id").asyncHandler {
        val type = getRouterParameter("type")
        val id = getRouterParameter("id")
        log.info("req type: $type, id: $id")
        writeJson(Response("ok", 200, Product(id, type))).end()
    }.router().get("/jsonMsg").asyncHandler {
        writeJson(Response<String>("fuck xxx", 33)).end()
    }.listen("localhost", 8080)
}

