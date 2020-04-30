package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.server.HttpServerFactory
import com.fireflysource.net.http.server.impl.router.handler.CorsConfig
import com.fireflysource.net.http.server.impl.router.handler.CorsHandler
import com.fireflysource.net.http.server.impl.router.handler.FileConfig
import com.fireflysource.net.http.server.impl.router.handler.FileHandler
import java.nio.file.Paths
import java.util.*

/*
wrk -t6 -c12 -d60s --latency http://localhost:9999/test
Running 1m test @ http://localhost:9999/test
6 threads and 12 connections
Thread Stats   Avg      Stdev     Max   +/- Stdev
Latency   140.75us   41.13us   2.74ms   97.43%
Req/Sec    13.92k   837.61    17.39k    75.68%
Latency Distribution
50%  137.00us
75%  144.00us
90%  153.00us
99%  247.00us
4993095 requests in 1.00m, 419.04MB read
Requests/sec:  83080.51
Transfer/sec:      6.97MB
*/
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