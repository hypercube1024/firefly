package com.firefly.example.kotlin.websocket

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.server.http2.router.handler.file.StaticFileHandler
import com.firefly.utils.concurrent.Schedulers
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) {
    val scheduler = Schedulers.createScheduler()
    val p = Paths.get(HttpServer::class.java.getResource("/").toURI())

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            paths = listOf("/favicon.ico", "/static/*")
            handler(StaticFileHandler(p.toAbsolutePath().toString()))
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/"
            asyncHandler { renderTemplate("template/websocket/index.mustache") }
        }

        webSocket("/helloWebSocket") {
            onConnect {
                val future = scheduler.scheduleAtFixedRate(
                        { it.sendText("Current time: " + Date()) },
                        0, 1, TimeUnit.SECONDS)
                it.onClose { future.cancel() }
            }

            onText { text, _ ->
                println("Server received: $text")
            }
        }
    }.listen("localhost", 8080)
}