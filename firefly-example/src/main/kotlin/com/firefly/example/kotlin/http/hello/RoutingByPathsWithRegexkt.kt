package com.firefly.example.kotlin.http.hello

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.http.*
import kotlinx.coroutines.experimental.runBlocking

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.GET
            regexPath = "/product(.*)"

            asyncHandler {
                val matched = getRegexGroup(1)
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

    val resp = firefly.httpClient().get("http://$host:$port/product/orange").asyncSubmit()
    println(resp.status)
    println(resp.stringBody)
}