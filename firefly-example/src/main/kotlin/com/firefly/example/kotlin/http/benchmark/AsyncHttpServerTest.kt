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
        listOf(
            Item("foo", 33.2),
            Item("beer", 5.99),
            Item("cola", 2.5),
            Item("mineral water", 2.0)
              )
}

/**
 * OS X 10.13, java version "1.8.0_144"
 * log level: INFO
 * JVM arguments: -XX:+UseG1GC -Xmx1024m -Xms1024m
 *
 * wrk -t8 -c32 -d60s --latency http://127.0.0.1:4455/
 * Running 1m test @ http://127.0.0.1:4455/
 * 8 threads and 32 connections
 * Thread Stats   Avg      Stdev     Max   +/- Stdev
 * Latency   462.09us  120.44us   6.44ms   89.37%
 * Req/Sec     8.60k   347.22     9.36k    71.38%
 * Latency Distribution
 * 50%  458.00us
 * 75%  506.00us
 * 90%  545.00us
 * 99%  718.00us
 * 4116689 requests in 1.00m, 482.90MB read
 * Requests/sec:  68496.13
 * Transfer/sec:      8.03MB
 *
 * wrk -t8 -c32 -d60s --latency http://127.0.0.1:4455/items
 * Running 1m test @ http://127.0.0.1:4455/items
 * 8 threads and 32 connections
 * Thread Stats   Avg      Stdev     Max   +/- Stdev
 * Latency   482.95us  121.53us   5.29ms   88.01%
 * Req/Sec     8.24k   308.00     9.01k    69.16%
 * Latency Distribution
 * 50%  484.00us
 * 75%  524.00us
 * 90%  561.00us
 * 99%  782.00us
 * 3940530 requests in 1.00m, 1.50GB read
 * Requests/sec:  65566.46
 * Transfer/sec:     25.51MB
 *
 * wrk -t8 -c32 -d60s --latency http://127.0.0.1:4455/items.json
 * Running 1m test @ http://127.0.0.1:4455/items.json
 * 8 threads and 32 connections
 * Thread Stats   Avg      Stdev     Max   +/- Stdev
 * Latency   475.15us  122.48us   6.39ms   88.77%
 * Req/Sec     8.38k   295.65     9.01k    77.58%
 * Latency Distribution
 * 50%  470.00us
 * 75%  517.00us
 * 90%  557.00us
 * 99%  785.00us
 * 4008998 requests in 1.00m, 1.05GB read
 * Requests/sec:  66704.40
 * Transfer/sec:     17.88MB
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

        router {
            httpMethod = HttpMethod.GET
            path = "/items.json"

            asyncHandler {
                writeJson(ItemRepository("drinks").repository()).end()
            }
        }
    }.listen("localhost", 4455)
}