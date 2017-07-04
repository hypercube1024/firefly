package com.firefly.kotlin.ext.benchmark

import com.firefly.codec.http2.model.HttpHeader.CONTENT_TYPE
import com.firefly.codec.http2.model.HttpMethod.GET
import com.firefly.codec.http2.model.MimeTypes.Type.TEXT_HTML_UTF_8
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.header

/**
 * @author Pengtao Qiu
 */
class ItemRepository(var name: String) {

    fun repository(): List<Item> = listOf(
            Item("foo", 33.2),
            Item("beer", 5.99),
            Item("cola", 2.5),
            Item("mineral water", 2.0))

    data class Item(var name: String, var price: Double)
}

fun main(args: Array<String>) {
    HttpServer {
        router {
            httpMethod = GET
            path = "/"

            asyncHandler {
                end("hello world!")
            }
        }

        router {
            httpMethod = GET
            path = "/items"

            asyncHandler {
                header {
                    CONTENT_TYPE to TEXT_HTML_UTF_8.asString()
                }

                renderTemplate("template/items.mustache", ItemRepository("drinks"))
            }
        }

    }.listen("127.0.0.1", 4455)
}