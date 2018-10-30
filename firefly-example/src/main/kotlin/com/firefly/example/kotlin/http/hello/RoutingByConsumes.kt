package com.firefly.example.kotlin.http.hello

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.annotation.NoArg
import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.asyncSubmit
import com.firefly.kotlin.ext.http.getJsonBody
import kotlinx.coroutines.runBlocking

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

            asyncHandler {
                val id = getPathParameter("id")
                val type = getWildcardMatchedResult(0)
                val car = getJsonBody<Car>()
                end("Update resource $id: $car. The content type is $type/json")
            }
        }
    }.listen(host, port)

    val resp = firefly.httpClient().put("http://$host:$port/product/20")
        .jsonBody(Car(20, "My car", "black")).asyncSubmit()
    println(resp.stringBody)
}

@NoArg
data class Car(
    var id: Long,
    var name: String,
    var color: String
              )