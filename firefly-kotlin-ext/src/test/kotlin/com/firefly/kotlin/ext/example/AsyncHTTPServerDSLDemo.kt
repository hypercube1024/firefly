package com.firefly.kotlin.ext.example

import com.firefly.codec.http2.model.HttpField
import com.firefly.codec.http2.model.HttpHeader.SERVER
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.codec.http2.model.HttpMethod.GET
import com.firefly.codec.http2.model.HttpMethod.POST
import com.firefly.codec.http2.model.HttpStatus.Code.OK
import com.firefly.kotlin.ext.annotation.NoArg
import com.firefly.kotlin.ext.http.*
import com.firefly.kotlin.ext.log.Log
import com.firefly.kotlin.ext.log.info
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import java.util.concurrent.TimeUnit

/**
 * Asynchronous HTTP server DSL examples
 *
 * @author Pengtao Qiu
 */

private val log = Log.getLogger { }

private val threadLocal = ThreadLocal<String>()



@NoArg
data class Product(var id: String, var type: String)

fun main(args: Array<String>) {
    val server = HttpServer {
        router {
            httpMethod = HttpMethod.GET
            path = "/"

            asyncHandler {
                end("hello world!")
            }
        }

        router {
            httpMethods = listOf(GET, POST)
            path = "/product/:type/:id"

            asyncHandler {
                statusLine {
                    status = OK.code
                    reason = OK.message
                }

                header {
                    "My-Header" to "Ohh nice"
                    SERVER to "Firefly kotlin DSL server"
                    +HttpField("Add-My-Header", "test add")
                }

                trailer {
                    "You-are-trailer" to "Crane ....."
                }

                val type = getRouterParameter("type")
                val id = getRouterParameter("id")
                log.info { "req type: $type, id: $id" }

                writeJson(Response("ok", 200, Product(id, type))).end()
            }
        }

        router {
            path = "/jsonMsg"

            asyncHandler {
                writeJson(Response<String>("fuck xxx", 33)).end()
            }
        }

        router {
            path = "/threadLocal/delay"
            httpMethod = GET

            asyncHandler {
                threadLocal.set("reqId_001")
                log.info("${uri.path} -> var: ${threadLocal.get()}, job: ${it[Job]}")

                delay(10, TimeUnit.SECONDS) // simulate I/O wait
                threadLocal.set(null) // clean thread local variable

                end("${uri.path} -> var:  ${threadLocal.get()}, job: ${it[Job]}")
            }
        }

        router {
            path = "/otherReq"
            httpMethod = GET

            asyncHandler {
                log.info("${uri.path} -> var: ${threadLocal.get()}, job: ${it[Job]}")
                runBlocking(it) {
                    log.info("${uri.path}, new coroutine 1 -> var: ${threadLocal.get()}, job: ${it[Job]}")
                }

                runBlocking {
                    log.info("${uri.path}, new coroutine 2 -> var: ${threadLocal.get()}, job: ${it[Job]}")
                }
                end("${uri.path} -> var: ${threadLocal.get()}, job: ${it[Job]}")
            }
        }
    }

    server.addRouters {
        router {
            httpMethod = POST
            path = "/product"
            consumes = "application/json"

            log.info { "setup router ($this)" }

            asyncHandler {
                val product = getJsonBody<Request<Product>>()
                writeJson(Response("mmp ok", 33, product.toString())).end()
            }
        }
    }

    server.listen("localhost", 8080)
}