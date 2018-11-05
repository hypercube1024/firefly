package com.firefly.example.kotlin.http.hello

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.asyncSubmit
import kotlinx.coroutines.runBlocking

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/hello"

            asyncHandler {
                end("hello world")
            }
        }
    }.listen(host, port)

    val resp = firefly.httpClient().get("http://$host:$port/hello").asyncSubmit()
    println(resp.status)
    println(resp.stringBody)
}