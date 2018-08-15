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
class TestWebJar {

    @Test
    fun test() = runBlocking {
        val host = "localhost"
        val port = RandomUtils.random(3000, 65534).toInt()
        val handler = AsyncWebJarHandler()

        val s = HttpServer {
            router {
                httpMethod = HttpMethod.GET
                path = "/webjars/*"

                asyncHandler(handler)
            }
        }.enableSecureConnection()
        s.listen(host, port)

        val c = firefly.createHTTPsClient()
        val resp = c.get("https://$host:$port/webjars/mdui/0.4.0/package.json").asyncSubmit()
        println(resp.status)
        println(resp.stringBody)
        assertEquals(200, resp.status)
        assertEquals(resp.stringBody.isNotEmpty(), true)
        c.stop()
        s.stop()
    }
}

