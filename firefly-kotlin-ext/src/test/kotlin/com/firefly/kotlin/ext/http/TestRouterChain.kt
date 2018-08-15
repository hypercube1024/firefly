package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.HttpMethod
import com.firefly.codec.http2.model.HttpStatus
import com.firefly.kotlin.ext.common.firefly
import com.firefly.utils.RandomUtils
import kotlinx.coroutines.experimental.TimeoutCancellationException
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestRouterChain {

    @Test
    fun test() = runBlocking {
        val host = "localhost"
        val port = RandomUtils.random(3000, 65534).toInt()

        val s = HttpServer {
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
        }.enableSecureConnection()
        s.listen(host, port)

        val c = firefly.createHTTPsClient()
        val resp = c.get("https://$host:$port/chain").asyncSubmit()
        println(resp.status)
        println(resp.stringBody)
        assertEquals(200, resp.status)
        assertEquals("into chain1\n" +
                "into chain2\n" +
                "into chain3\n" +
                "complete chain3\n" +
                "complete chain2\n" +
                "complete chain1\n", resp.stringBody)
        c.stop()
        s.stop()
    }

    @Test
    fun testErrorAndTimeout() = runBlocking {
        val host = "localhost"
        val port = RandomUtils.random(3000, 65534).toInt()

        val s = HttpServer {
            router {
                httpMethod = HttpMethod.GET
                path = "/*"

                asyncHandler {
                    try {
                        asyncNext<Unit>(1, TimeUnit.SECONDS)
                        end()
                    } catch (e: TimeoutCancellationException) {
                        statusLine {
                            status = HttpStatus.GATEWAY_TIMEOUT_504
                        }
                        end("The server is overloaded")
                    } catch (e: Exception) {
                        statusLine {
                            status = HttpStatus.INTERNAL_SERVER_ERROR_500
                        }
                        end("The server exception. ${e.message}")
                    }
                }
            }

            router {
                httpMethod = HttpMethod.GET
                path = "/testError"

                asyncCompleteHandler {
                    throw IllegalArgumentException("Hoo!")
                }
            }

            router {
                httpMethod = HttpMethod.GET
                path = "/testTimeout"

                asyncCompleteHandler {
                    delay(2, TimeUnit.SECONDS)
                    write("ok!")
                }
            }

            router {
                httpMethod = HttpMethod.GET
                path = "/test"

                asyncCompleteHandler {
                    write("ok!")
                }
            }
        }.enableSecureConnection()
        s.listen(host, port)
        val c = firefly.createHTTPsClient()

        val resp = c.get("https://$host:$port/testTimeout").asyncSubmit()
        println(resp.status)
        println(resp.stringBody)
        assertEquals(504, resp.status)
        assertEquals("The server is overloaded", resp.stringBody)

        val resp1 = c.get("https://$host:$port/testError").asyncSubmit()
        println(resp1.status)
        println(resp1.stringBody)
        assertEquals(500, resp1.status)
        assertEquals("The server exception. Hoo!", resp1.stringBody)

        val resp2 = c.get("https://$host:$port/test").asyncSubmit()
        println(resp2.status)
        println(resp2.stringBody)
        assertEquals(200, resp2.status)
        assertEquals("ok!", resp2.stringBody)
        c.stop()
        s.stop()
    }
}