package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.HttpField
import com.firefly.codec.http2.model.HttpHeader.*
import com.firefly.codec.http2.model.HttpMethod.GET
import com.firefly.codec.http2.model.HttpStatus.Code.UNAUTHORIZED
import com.firefly.codec.http2.model.MimeTypes
import com.firefly.kotlin.ext.common.CoroutineLocal
import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.example.Response
import com.firefly.server.http2.router.RoutingContext
import com.firefly.utils.RandomUtils
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestServerDSL {

    @Test
    fun testDSL(): Unit = runBlocking {
        val host = "localhost"
        val port = RandomUtils.random(3000, 65534).toInt()

        HttpServer {
            router {
                httpMethod = GET
                path = "/test/headerAndTrailer"

                asyncHandler {
                    header {
                        "My-Header" to "Ohh nice"
                        SERVER to "Firefly kotlin DSL server"
                        +HttpField("Add-My-Header", "test add")
                    }

                    trailer {
                        "You-are-trailer" to "Crane ....."
                    }

                    writeJson(Response("ok", 0, "test ok")).end()
                }
            }

            router {
                httpMethod = GET
                path = "/test/statusLine"

                asyncHandler {
                    statusLine {
                        status = UNAUTHORIZED.code
                        reason = UNAUTHORIZED.message
                    }

                    end("Forbid access")
                }
            }

            router {
                httpMethod = GET
                path = "/test/gzipMsg"

                asyncHandler {

                    header {
                        CONTENT_TYPE to MimeTypes.Type.TEXT_PLAIN_UTF_8.asString()
                        CONTENT_ENCODING to "gzip"
                    }

                    GZIPOutputStream(response.outputStream).safeUse {
                        it.write("gzip msg".toByteArray(StandardCharsets.UTF_8))
                    }
                }
            }

        }.listen(host, port)

        val url = "http://$host:$port"
        val client = firefly.httpClient()

        val r0 = client.get("$url/test/headerAndTrailer").asyncSubmit()
        assertEquals("test add", r0.fields["Add-My-Header"])
        assertEquals("Ohh nice", r0.fields["My-Header"])
        assertEquals("Firefly kotlin DSL server", r0.fields[SERVER])
        assertEquals("Crane .....", r0.getTrailer()["You-are-trailer"])

        val r1 = client.get("$url/test/statusLine").asyncSubmit()
        assertEquals(UNAUTHORIZED.code, r1.status)
        assertEquals(UNAUTHORIZED.message, r1.reason)
        assertEquals("Forbid access", r1.stringBody)

        val r2 = client.get("$url/test/gzipMsg").asyncSubmit()
        assertEquals("gzip msg", r2.stringBody)
    }

    @Test
    fun testRequestCtx(): Unit = runBlocking {
        val host = "localhost"
        val port = RandomUtils.random(3000, 65534).toInt()
        val reqCtx = CoroutineLocal<RoutingContext>()

        fun testReqId() = assertEquals(20, reqCtx.get()?.getAttr<Int>("reqId"))

        HttpServer(reqCtx) {
            router {
                httpMethod = GET
                path = "/test/*"

                asyncHandler {
                    setAttribute("reqId", 20)
                    next()
                }
            }

            router {
                httpMethod = GET
                path = "/test/product/:id"

                asyncHandler {
                    val reqId = getAttr<Int>("reqId")
                    assertEquals(20, reqId)
                    testReqId()
                    end("reqId: $reqId")
                }
            }
        }.listen(host, port)

        val url = "http://$host:$port"
        val client = firefly.httpClient()

        val r0 = client.get("$url/test/product/3").asyncSubmit()
        assertEquals("reqId: 20", r0.stringBody)
    }

    @Test
    fun testRouterChain(): Unit = runBlocking {
        val host = "localhost"
        val port = RandomUtils.random(3000, 65534).toInt()

        HttpServer {
            router {
                httpMethod = GET
                path = "/test/chain/*"

                asyncHandler {
                    setAttribute("userId", 33)
                    promise<String>({
                        write("router 1 success\r\n").end(it)
                    }, {
                        write("${it?.message}").end()
                    }).next()
                }
            }

            router {
                httpMethod = GET
                path = "/test/chain/task/:id"

                asyncHandler {
                    write("enter router 2\r\n").succeed("User ${getAttr<Int>("userId")} gets task ${getRouterParameter("id")}\r\n")
                }
            }
        }.listen(host, port)

        val url = "http://$host:$port"
        val client = firefly.httpClient()
        val r0 = client.get("$url/test/chain/task/70").asyncSubmit()
        val expectedBody = "enter router 2\r\nrouter 1 success\r\nUser 33 gets task 70\r\n"
        println(r0.stringBody)
        assertEquals(expectedBody, r0.stringBody)
    }

}