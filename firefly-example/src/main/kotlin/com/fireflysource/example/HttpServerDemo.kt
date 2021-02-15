package com.fireflysource.example

import com.fireflysource.net.http.server.HttpServerFactory
import com.fireflysource.net.http.server.impl.router.handler.CorsConfig
import com.fireflysource.net.http.server.impl.router.handler.CorsHandler
import com.fireflysource.net.http.server.impl.router.handler.FileHandler

/*
Intel i5 1.4GHz 16GB macbook pro 13

wrk -t4 -c16 -d60s --latency http://localhost:9999/test
Running 1m test @ http://localhost:9999/test
  4 threads and 16 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   170.83us   34.94us   4.30ms   77.90%
    Req/Sec    22.74k     0.99k   26.71k    81.82%
  Latency Distribution
     50%  170.00us
     75%  189.00us
     90%  207.00us
     99%  252.00us
  5438229 requests in 1.00m, 456.39MB read
Requests/sec:  90486.34
Transfer/sec:      7.59MB
*/
fun main() {
    val httpServer = HttpServerFactory.create()
    val corsConfig = CorsConfig("*")

    httpServer
        .router().path("*").handler(CorsHandler(corsConfig))
        .router().paths(listOf("/favicon.ico", "/poem.html", "/poem.txt"))
        .handler(FileHandler.createFileHandlerByResourcePath("files"))
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