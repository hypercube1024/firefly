package com.firefly.example.kotlin.http.hello

import com.firefly.codec.http2.model.HttpHeader
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.asyncSubmit
import com.firefly.kotlin.ext.http.getJsonBody
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
                val car = getJsonBody<Car>()
                end("Update resource $id: $car")
            }
        }

        router {
            httpMethod = HttpMethod.PUT
            path = "/product/:id"
            consumes = "*/json"
            produces = "application/json"

            asyncHandler {
                writeJson(getJsonBody<Car>()).end()
            }
        }
    }.listen(host, port)

    val text = firefly.httpClient().put("http://$host:$port/product/20")
            .put(HttpHeader.ACCEPT, "text/plain, application/json;q=0.9, */*;q=0.8")
            .jsonBody(Car(20, "My car", "black"))
            .asyncSubmit()
    println(text.stringBody)

    val json = firefly.httpClient().put("http://$host:$port/product/20")
            .put(HttpHeader.ACCEPT, "application/json, text/plain, */*;q=0.8")
            .jsonBody(Car(20, "My car", "black"))
            .asyncSubmit()
    println(json.stringBody)
}