package com.fireflysource.example

import com.fireflysource.`$`

fun main() {
    `$`.httpServer()
        .router().get("/").handler { ctx -> ctx.write("Hello world! ").next() }
        .router().get("/").handler { ctx -> ctx.end("The router demo.") }
        .listen("localhost", 8090)
}