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