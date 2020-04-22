package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.server.HttpServerFactory
import com.fireflysource.net.http.server.impl.router.handler.CorsConfig
import com.fireflysource.net.http.server.impl.router.handler.CorsHandler

fun main() {
    val httpServer = HttpServerFactory.create()
    val corsConfig = CorsConfig("*")

    httpServer
        .router().path("*").handler(CorsHandler(corsConfig))
        .router().post("/cors-preflight/*").handler {
            it.end(
                """
                    |{"status": "ok"}
                """.trimMargin()
            )
        }
        .router().get("/test").handler {
            it.end("Welcome")
        }
        .listen("localhost", 9999)
}