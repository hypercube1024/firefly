package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.server.HttpServerFactory
import com.fireflysource.net.http.server.impl.router.handler.CorsConfig
import com.fireflysource.net.http.server.impl.router.handler.CorsHandler
import com.fireflysource.net.http.server.impl.router.handler.FileConfig
import com.fireflysource.net.http.server.impl.router.handler.FileHandler
import java.nio.file.Paths
import java.util.*

/*
Intel i5 1.4GHz 16GB macbook pro 13

wrk -t4 -c16 -d60s --latency http://localhost:9999/test
Running 1m test @ http://localhost:9999/test
  4 threads and 16 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   176.68us   34.50us   4.43ms   76.07%
    Req/Sec    22.00k     1.10k   25.58k    85.36%
  Latency Distribution
     50%  175.00us
     75%  195.00us
     90%  214.00us
     99%  261.00us
  5262054 requests in 1.00m, 441.61MB read
Requests/sec:  87555.25
Transfer/sec:      7.35MB
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