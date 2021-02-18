package com.fireflysource.example

import com.fireflysource.`$`

fun main() {
    `$`.httpServer()
        .router().post("/product").handler {
            throw IllegalStateException("Create product exception")
        }
        .listen("localhost", 8090)

    val url = "http://localhost:8090"
    `$`.httpClient().post("$url/product/").submit()
        .thenAccept { response -> println(response) }
}