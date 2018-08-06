package com.firefly.example.kotlin.http.hello

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.asyncNext
import com.firefly.kotlin.ext.http.asyncSubmit
import com.firefly.kotlin.ext.http.asyncSucceed
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
            path = "/handlerChain"

            asyncHandler {
                write("Into the first handler.\r\n")
                val r = asyncNext<String>()
                write(r.second)
                end("Complete the first handler.")
            }
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/handlerChain"

            asyncHandler {
                write("Into the second handler.\r\n")
                val r = asyncNext<String>()
                write(r.second)
                asyncSucceed("Complete the second handler.\r\n")
            }
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/handlerChain"

            asyncHandler {
                write("Into the last handler.\r\n")
                asyncSucceed("Complete the last handler.\r\n")
            }
        }
    }.listen(host, port)

    val resp = firefly.httpClient().get("http://$host:$port/handlerChain").asyncSubmit()
    println(resp.status)
    println(resp.stringBody)
}