package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.common.firefly
import com.firefly.utils.RandomUtils
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestRouterChain {

    @Test
    fun test() = runBlocking {
        val host = "localhost"
        val port = RandomUtils.random(3000, 65534).toInt()

        HttpServer {
            router {
                httpMethod = HttpMethod.GET
                path = "/chain"

                asyncHandler {
                    write("into chain1\n")
                    asyncNext<Unit>()
                    end("complete chain1\n")
                }
            }

            router {
                httpMethod = HttpMethod.GET
                path = "/chain"

                asyncCompleteHandler {
                    write("into chain2\n")
                    asyncNext<Unit>()
                    write("complete chain2\n")
                }
            }

            router {
                httpMethod = HttpMethod.GET
                path = "/chain"

                asyncCompleteHandler {
                    write("into chain3\n")
                    write("complete chain3\n")
                }
            }
        }.enableSecureConnection().listen(host, port)

        val resp = firefly.httpsClient().get("https://$host:$port/chain").asyncSubmit()
        println(resp.status)
        println(resp.stringBody)
        assertEquals(200, resp.status)
        assertEquals("into chain1\n" +
                "into chain2\n" +
                "into chain3\n" +
                "complete chain3\n" +
                "complete chain2\n" +
                "complete chain1\n", resp.stringBody)
    }
}