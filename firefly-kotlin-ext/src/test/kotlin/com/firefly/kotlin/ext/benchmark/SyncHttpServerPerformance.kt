package com.firefly.kotlin.ext.benchmark

import com.firefly.codec.http2.model.HttpHeader
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.codec.http2.model.MimeTypes
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.header

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) {
    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/"

            syncHandler {
                end("hello world!")
            }
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/items"

            syncHandler {
                header {
                    HttpHeader.CONTENT_TYPE to MimeTypes.Type.TEXT_HTML_UTF_8.asString()
                }

                renderTemplate("template/items.mustache", ItemRepository("drinks"))
            }
        }

    }.listen("127.0.0.1", 4455)
}