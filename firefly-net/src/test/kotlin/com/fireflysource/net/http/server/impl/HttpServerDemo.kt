package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.server.HttpServerFactory
import com.fireflysource.net.http.server.impl.router.handler.CorsConfig
import com.fireflysource.net.http.server.impl.router.handler.CorsHandler
import com.fireflysource.net.http.server.impl.router.handler.FileConfig
import com.fireflysource.net.http.server.impl.router.handler.FileHandler
import java.nio.file.Paths
import java.util.*

fun main() {
    val httpServer = HttpServerFactory.create()
    val corsConfig = CorsConfig("*")
    val path = Optional.ofNullable(FileHandler::class.java.classLoader.getResource("files"))
        .map { it.toURI() }
        .map { Paths.get(it) }
        .map { it.toString() }
        .orElse("")
    val fileConfig = FileConfig(path)

    httpServer
        .router().path("*").handler(CorsHandler(corsConfig))
        .router().paths(listOf("/favicon.ico", "/poem.html", "/poem.txt")).handler(FileHandler(fileConfig))
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