package com.fireflysource.example

import com.fireflysource.`$`
import com.fireflysource.net.http.common.model.HttpStatus

fun main() {
    `$`.httpServer()
        .onException { ctx, exception ->
            ctx.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500)
                .end("The server exception. ${exception.message}")
        }
        .router().post("/product").handler {
            throw IllegalStateException("Create product exception")
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().post("$url/product/").submit()
        .thenAccept { response -> println(response) }
}