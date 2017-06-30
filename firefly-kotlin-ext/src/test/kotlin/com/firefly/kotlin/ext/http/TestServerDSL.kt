package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.HttpField
import com.firefly.codec.http2.model.HttpHeader.SERVER
import com.firefly.codec.http2.model.HttpMethod.GET
import com.firefly.kotlin.ext.common.firefly
import com.firefly.kotlin.ext.example.Response
import com.firefly.utils.RandomUtils
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestServerDSL {

    @Test
    fun testDSL(): Unit = runBlocking {
        val host = "localhost"
        val port = RandomUtils.random(3000, 65534).toInt()
        val url = "http://$host:$port"
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

        }.listen(host, port)

        val client = firefly.httpClient()

        val r0 = client.get("$url/test/headerAndTrailer").asyncSubmit()
        assertEquals("test add", r0.fields["Add-My-Header"])
        assertEquals("Ohh nice", r0.fields["My-Header"])
        assertEquals("Firefly kotlin DSL server", r0.fields[SERVER])
        assertEquals("Crane .....", r0.trailerSupplier.get()["You-are-trailer"])
    }
}