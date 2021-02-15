package com.fireflysource.example

import com.fireflysource.`$`

fun main() {
    `$`.httpServer()
        .router().get("/").handler { ctx -> ctx.end("Hello https! ") }
        .enableSecureConnection()
        .listen("localhost", 8090)

    `$`.httpClient().get("https://localhost:8090/").submit()
        .thenAccept { response -> println(response.stringBody) }
}