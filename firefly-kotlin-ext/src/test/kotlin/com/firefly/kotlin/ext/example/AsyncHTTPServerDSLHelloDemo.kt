package com.firefly.kotlin.ext.example

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.http.HttpServer

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) {
    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/"

            asyncHandler {
                end("hello world!")
            }
        }
    }.listen("localhost", 8080)
}