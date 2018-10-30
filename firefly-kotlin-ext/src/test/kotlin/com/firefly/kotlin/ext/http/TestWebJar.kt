package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.HttpHeader
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.kotlin.ext.common.firefly
import com.firefly.utils.RandomUtils
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */
class TestWebJar {

    @Test
    fun testDisableGzip() = runBlocking {
        testWebJar(false)
    }

    @Test
    fun testEnableGzip() = runBlocking {
        testWebJar(true)
    }

    private suspend fun testWebJar(enableGzip: Boolean) {
        val host = "localhost"
        val port = RandomUtils.random(3000, 65534).toInt()
        val handler = AsyncWebJarHandler(enableGzip)

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
        println(resp.contentLength)
        println(resp.stringBody)
        assertEquals(200, resp.status)
        assertEquals(resp.stringBody.isNotEmpty(), true)
        assertEquals(Objects.equals(resp.fields[HttpHeader.CONTENT_ENCODING], "gzip"), enableGzip)

        val notFoundResp = c.get("https://$host:$port/webjars/mdui/0.4.0/xxx.json").asyncSubmit()
        println(notFoundResp.status)
        println(notFoundResp.stringBody)
        assertEquals(404, notFoundResp.status)

        c.stop()
        s.stop()
    }
}

