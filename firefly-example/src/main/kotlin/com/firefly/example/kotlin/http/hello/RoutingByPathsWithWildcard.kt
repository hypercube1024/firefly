package com.firefly.example.kotlin.http.hello

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.asyncNext
import com.firefly.kotlin.ext.http.asyncSubmit
import com.firefly.kotlin.ext.http.asyncSucceed
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
            path = "/product*"

            asyncHandler {
                val matched = getWildcardMatchedResult(0)
                write("Intercept the product: $matched\r\n")
                asyncNext<String> {
                    end(it)
                }
            }
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/product/:type"

            asyncHandler {
                val type = getPathParameter("type")
                write("List $type\r\n")
                asyncSucceed("List $type success")
            }
        }
    }.listen(host, port)

    val resp = firefly.httpClient().get("http://$host:$port/product/apple").asyncSubmit()
    println(resp.status)
    println(resp.stringBody)
}