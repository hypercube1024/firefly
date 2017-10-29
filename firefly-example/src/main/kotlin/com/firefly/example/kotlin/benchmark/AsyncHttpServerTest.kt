package com.firefly.example.kotlin.benchmark

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
 * wrk -t8 -c32 -d60s http://127.0.0.1:4455/items
 * Running 1m test @ http://127.0.0.1:4455/items
 * 8 threads and 32 connections
 * Thread Stats   Avg      Stdev     Max   +/- Stdev
 * Latency   530.31us  135.35us  10.91ms   93.14%
 * Req/Sec     7.54k   428.68     8.72k    82.15%
 * 3609078 requests in 1.00m, 1.37GB read
 * Requests/sec:  60050.09
 * Transfer/sec:     23.37MB
 */
fun main(args: Array<String>) {
    HttpServer {
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