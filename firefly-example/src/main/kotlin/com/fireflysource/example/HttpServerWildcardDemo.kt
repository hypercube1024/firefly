package com.fireflysource.example

import com.fireflysource.`$`

fun main() {
    `$`.httpServer()
        .router().put("/product/*/*").handler { ctx ->
            val type = ctx.getPathParameter(0)
            val id = ctx.getPathParameter(1)
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