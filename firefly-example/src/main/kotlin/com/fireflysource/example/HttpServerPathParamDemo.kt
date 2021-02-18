package com.fireflysource.example

import com.fireflysource.`$`
import com.fireflysource.net.http.common.model.HttpStatus.NOT_FOUND_404

fun main() {
    `$`.httpServer()
        .router().get("/product/:id").handler { ctx ->
            when (val id = ctx.getPathParameter("id")) {
                "1" -> ctx.end("Apple")
                "2" -> ctx.end("Orange")
                else -> ctx.setStatus(NOT_FOUND_404).end("The product $id not found.")
            }
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().get("$url/product/1").submit()
        .thenAccept { response -> println(response.stringBody) }

    `$`.httpClient().get("$url/product/2").submit()
        .thenAccept { response -> println(response.stringBody) }

    `$`.httpClient().get("$url/product/3").submit()
        .thenAccept { response -> println(response.stringBody) }
}