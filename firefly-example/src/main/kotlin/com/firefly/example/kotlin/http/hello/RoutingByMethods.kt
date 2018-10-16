package com.firefly.example.kotlin.http.hello

import com.firefly.codec.http2.model.HttpMethod.*
import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.asyncSubmit
import kotlinx.coroutines.experimental.runBlocking

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) = runBlocking {
    val host = "localhost"
    val port = 8081

    HttpServer {
        router {
            httpMethod = GET
            path = "/product/:id"

            asyncHandler {
                val id = getPathParameter("id")
                end("Get the product $id.")
            }
        }

        router {
            httpMethod = POST
            path = "/product"

            asyncHandler {
                end("Create a new product: $stringBody")
            }
        }

        router {
            httpMethod = PUT
            path = "/product/:id"

            asyncHandler {
                val id = getPathParameter("id")
                end("Update the product $id: $stringBody")
            }
        }

        router {
            httpMethod = DELETE
            path = "/product/:id"

            asyncHandler {
                val id = getPathParameter("id")
                end("Delete the product $id")
            }
        }
    }.listen(host, port)

    val getResp = firefly.httpClient().get("http://$host:$port/product/20").asyncSubmit()
    println(getResp.stringBody)

    val postResp = firefly.httpClient().post("http://$host:$port/product")
        .body("Car 20. The color is red.").asyncSubmit()
    println(postResp.stringBody)

    val putResp = firefly.httpClient().put("http://$host:$port/product/20")
        .body("Change the color from red to black.").asyncSubmit()
    println(putResp.stringBody)

    val delResp = firefly.httpClient().delete("http://$host:$port/product/20").asyncSubmit()
    println(delResp.stringBody)
}