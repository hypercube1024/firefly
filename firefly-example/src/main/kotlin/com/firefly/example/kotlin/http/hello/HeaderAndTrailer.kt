package com.firefly.example.kotlin.http.hello

import com.firefly.codec.http2.model.*
import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.http.*
import com.firefly.utils.io.BufferUtils
import kotlinx.coroutines.runBlocking

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = HttpMethod.POST
            path = "/product"

            asyncHandler {
                statusLine {
                    status = HttpStatus.Code.OK.code
                    reason = HttpStatus.Code.OK.message
                }

                header {
                    HttpHeader.SERVER to "Firefly Kotlin Server"
                    +HttpField("Woo", "Ohh nice")
                }

                trailer {
                    "Home-Page" to "www.fireflysource.com"
                }

                val trailer = request.trailerSupplier.get()
                end(
                    "The server received:\r\n" +
                            "$stringBody\r\n" +
                            "Sender: ${trailer["Sender"]}\r\n" +
                            "${trailer["Signature"]}\r\n"
                   )
            }
        }
    }.listen(host, port)

    val resp = firefly.httpClient()
        .post("http://$host:$port/product")
        .output {
            it.use {
                it.write(
                    BufferUtils.toBuffer(
                        "IKBC C87\r\n" +
                                "Cherry G80-3000\r\n"
                                        )
                        )
            }
        }
        .setTrailerSupplier {
            val fields = HttpFields()
            fields.put("Sender", "Firefly Kotlin Client")
            fields.put("Signature", "It does not do to dwell on dreams and forget to live.")
            fields
        }.asyncSubmit()

    println(resp.status)
    println(resp.fields)
    println(resp.trailerSupplier.get()["Home-Page"])
    println(resp.stringBody)
}