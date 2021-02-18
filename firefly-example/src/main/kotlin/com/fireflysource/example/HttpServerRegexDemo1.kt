package com.fireflysource.example

import com.fireflysource.`$`
import com.fireflysource.net.http.common.model.HttpMethod

fun main() {
    `$`.httpServer()
        .router().method(HttpMethod.PUT).pathRegex("/product/(.*)/(.*)").handler { ctx ->
            val type = ctx.getPathParameterByRegexGroup(1)
            val id = ctx.getPathParameterByRegexGroup(2)
            val product = ctx.stringBody
            ctx.end("Put product success. id: $id, type: $type, product: $product")
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().put("$url/product/fruit/1").body("Apple").submit()
        .thenAccept { response -> println(response.stringBody) }

    `$`.httpClient().put("$url/product/book/1").body("Tom and Jerry").submit()
        .thenAccept { response -> println(response.stringBody) }

    `$`.httpClient().put("$url/product/book/2").body("The Three-Body Problem").submit()
        .thenAccept { response -> println(response.stringBody) }
}