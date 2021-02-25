package com.fireflysource.example

import com.fireflysource.`$`
import com.fireflysource.common.io.useAwait
import com.fireflysource.net.http.client.impl.connectAsync
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await

fun main() {
    `$`.httpServer()
        .router().get("/hello").handler { ctx -> ctx.end("Hello http! ") }
        .listen("localhost", 8090)

    `$`.httpClient().connectAsync("http://localhost:8090") { connection ->
        connection.useAwait {
            repeat(3) {
                val response = connection.get("/hello").submit().await()
                println("connection ${connection.id} received: ${response.stringBody}")
                delay(1000)
            }
        }
        println("connection ${connection.id} closed. ${connection.isClosed}")
    }
}