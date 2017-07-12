package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.HttpField
import com.firefly.codec.http2.model.HttpHeader.*
import com.firefly.codec.http2.model.HttpMethod.GET
import com.firefly.codec.http2.model.HttpStatus.Code.UNAUTHORIZED
import com.firefly.codec.http2.model.MimeTypes
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
    fun testRequestLocal(): Unit = runBlocking {
        val host = "localhost"
        val port = RandomUtils.random(3000, 65534).toInt()
        val reqLocal = ThreadLocal<RoutingContext>()

        fun testReqId() {
            val reqId = reqLocal.get()?.getAttribute("reqId") as Int
            assertEquals(20, reqId)
        }

        HttpServer(reqLocal) {
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
                    val reqId = getAttribute("reqId") as Int
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


}