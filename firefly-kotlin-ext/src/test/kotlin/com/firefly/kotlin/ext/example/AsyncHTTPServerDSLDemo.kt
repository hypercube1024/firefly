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
import com.firefly.server.http2.router.RoutingContext
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
private val requestLocal = ThreadLocal<RoutingContext>()

@NoArg
data class Product(var id: String, var type: String)

fun main(args: Array<String>) {
    val server = HttpServer(requestLocal) {
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
            httpMethod = GET
            path = "/threadLocal/delay"

            asyncHandler {
                threadLocal.set("reqId_001")
                setAttribute("reqId", "001")

                try {
                    log.info("${uri.path} -> var: ${threadLocal.get()}, job: ${it[Job]}")
                    testCoroutineCtx()

                    delay(10, TimeUnit.SECONDS) // simulate I/O wait

                    end("${uri.path} -> threadLocal:  ${threadLocal.get()}, job: ${it[Job]}, reqId: ${getAttribute("reqId")}")
                } finally {
                    threadLocal.set(null) // clean thread local variable
                    removeAttribute("reqId") // clean request id
                }
            }
        }

        router {
            httpMethod = GET
            path = "/otherReq"

            asyncHandler {
                log.info("${uri.path} -> var: ${threadLocal.get()}, job: ${it[Job]}")
                testCoroutineCtx()
                runBlocking(it) {
                    log.info("${uri.path}, new coroutine 1 -> var: ${threadLocal.get()}, job: ${it[Job]}")
                }

                runBlocking {
                    log.info("${uri.path}, new coroutine 2 -> var: ${threadLocal.get()}, job: ${it[Job]}")
                }
                end("${uri.path} -> var: ${threadLocal.get()}, job: ${it[Job]}, reqId: ${getAttribute("reqId")}")
            }
        }

        router {
            httpMethod = GET
            path = "/routerChain"

            asyncHandler {
                promise<String>({
                    write("router 1 success\r\n").end(it)
                }, {
                    write("${it?.message}").end()
                })

                setAttribute("reqId", 1000)
                write("enter router 1\r\n").next()
            }
        }

        router {
            httpMethod = GET
            path = "/routerChain"

            asyncHandler {
                val reqId = getAttribute("reqId") as Int
                promise<String> {
                    write("router 2 success, request id $reqId\r\n")
                }

                write("enter router 2, request id $reqId\r\n").next()
            }
        }

        router {
            httpMethod = GET
            path = "/routerChain"

            asyncHandler {
                val reqId = getAttribute("reqId") as Int
                promise<String> {
                    write("router 3 success, request id $reqId\r\n")
                }

                write("enter router 3, request id $reqId\r\n").succeed("request complete")
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

suspend fun testCoroutineCtx() {
    log.info("coroutine local ${requestLocal.get()?.uri?.path} -> ${requestLocal.get()?.getAttribute("reqId")}")
}