package com.firefly.example.kotlin.http.hello

import com.firefly.client.http2.SimpleResponse
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.codec.http2.model.HttpStatus.NOT_FOUND_404
import com.firefly.codec.http2.model.HttpStatus.OK_200
import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.asyncSubmit
import com.firefly.kotlin.ext.http.getRequestContext
import kotlinx.coroutines.runBlocking

/**
 * @author Pengtao Qiu
 */
const val host = "localhost"
const val port1 = 8081
const val port2 = 8082

fun main(args: Array<String>) = runBlocking {
    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/product"

            asyncHandler {
                val reqId = fields["Request-ID"]
                val name = getParameter("name")
                val p = this@HttpServer.server.configuration.port

                write("[$reqId-$p]: The product $name is not found. we will try to find it from the other server.\r\n")
                write("[$reqId-$p]: Please wait......\r\n")
                val resp = searchProduct(name)
                when (resp.status) {
                    OK_200 -> end(resp.stringBody)
                    NOT_FOUND_404 -> end("The product is not found on all servers")
                    else -> end("The server exception. ${resp.reason}")
                }
            }
        }
    }.listen(host, port1)

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/product"

            asyncHandler {
                val reqId = fields["Request-ID"]
                val name = getParameter("name")
                val p = this@HttpServer.server.configuration.port

                end("[$reqId-$p]: The product $name: Hannah\r\n")
            }
        }
    }.listen(host, port2)

    val resp = firefly.httpClient()
        .get("http://$host:$port1/product?name=Han")
        .put("Request-ID", "333").asyncSubmit()
    println(resp.status)
    println(resp.stringBody)
}

suspend fun searchProduct(name: String): SimpleResponse {
    val ctx = getRequestContext()
    val reqId = ctx?.fields?.get("Request-ID")
    return firefly.httpClient()
        .get("http://$host:$port2/product?name=$name")
        .put("Request-ID", reqId)
        .asyncSubmit()
}