package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.HttpMethod.*
import com.firefly.kotlin.ext.annotation.NoArg
import com.firefly.kotlin.ext.log.Log

/**
 * @author Pengtao Qiu
 */

private val log = Log.getLogger { }

@NoArg
data class Response<T>(var msg: String, var code: Int, var data: T? = null)

@NoArg
data class Request<T>(var token: String, var data: T? = null)

@NoArg
data class Product(var id: String, var type: String)

fun main(args: Array<String>) {
    val server = HttpServer {
        router {
            path = "/product/:type/:id"

            asyncHandler {
                val type = getRouterParameter("type")
                val id = getRouterParameter("id")
                log.info("req type: $type, id: $id")
                writeJson(Response("ok", 200, Product(id, type))).end()
            }
        }

        router {
            path = "/jsonMsg"

            asyncHandler {
                writeJson(Response<String>("fuck xxx", 33)).end()
            }
        }
    }

    server.addRouters {
        router {
            httpMethod = POST
            path = "/product"
            consumes = "application/json"

            asyncHandler {
                val product = getJsonBody<Request<Product>>()
                writeJson(Response("mmp ok", 33, product.toString())).end()
            }
        }
    }

    server.listen("localhost", 8080)
}