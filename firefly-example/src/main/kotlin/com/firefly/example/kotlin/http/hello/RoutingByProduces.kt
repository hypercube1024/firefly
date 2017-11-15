package com.firefly.example.kotlin.http.hello

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.getJsonBody
import com.firefly.kotlin.ext.http.getPathParameter
import com.firefly.kotlin.ext.http.getWildcardMatchedResult
import kotlinx.coroutines.experimental.runBlocking

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.PUT
            path = "/product/:id"
            consumes = "*/json"
            produces = "text/plain"

            asyncHandler {
                val id = getPathParameter("id")
                val type = getWildcardMatchedResult(0)
                val car = getJsonBody<Car>()
                end("Update resource $id: $car. The content type is $type/json")
            }
        }
    }
    println()
}