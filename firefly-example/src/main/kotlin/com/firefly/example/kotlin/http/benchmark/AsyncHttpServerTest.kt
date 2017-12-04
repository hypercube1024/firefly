package com.firefly.example.kotlin.http.benchmark

import com.firefly.codec.http2.model.HttpHeader.CONTENT_TYPE
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.codec.http2.model.MimeTypes
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.header

/**
 * @author Pengtao Qiu
 */

data class Item(val name: String, val price: Double)

data class ItemRepository(val name: String) {
    fun repository(): List<Item> =
            listOf(Item("foo", 33.2),
                   Item("beer", 5.99),
                   Item("cola", 2.5),
                   Item("mineral water", 2.0))
}

/**
 * OS X 10.13, java version "1.8.0_144"
 * log level: INFO
 * JVM arguments: -XX:+UseG1GC -Xmx1024m -Xms1024m
 *
 * wrk -t8 -c32 -d60s http://127.0.0.1:4455/
 * Running 1m test @ http://127.0.0.1:4455/
 * 8 threads and 32 connections
 * Thread Stats   Avg      Stdev     Max   +/- Stdev
 * Latency   473.34us  114.16us   5.82ms   90.43%
 * Req/Sec     8.43k   433.16     9.56k    76.87%
 * 4033097 requests in 1.00m, 473.09MB read
 * Requests/sec:  67105.43
 * Transfer/sec:      7.87MB
 *
 * wrk -t8 -c32 -d60s http://127.0.0.1:4455/items
 * Running 1m test @ http://127.0.0.1:4455/items
 * 8 threads and 32 connections
 * Thread Stats   Avg      Stdev     Max   +/- Stdev
 * Latency   502.37us  258.08us  32.52ms   98.91%
 * Req/Sec     8.01k   323.65     8.98k    73.92%
 * 3834331 requests in 1.00m, 1.46GB read
 * Requests/sec:  63798.77
 * Transfer/sec:     24.82MB
 */
fun main(args: Array<String>) {
    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/"

            asyncHandler { end("hello world!") }
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/items"

            asyncHandler {
                header {
                    CONTENT_TYPE to MimeTypes.Type.TEXT_HTML_UTF_8.asString()
                }

                renderTemplate("template/benchmark/items.mustache", ItemRepository("drinks"))
            }
        }
    }.listen("127.0.0.1", 4455)
}