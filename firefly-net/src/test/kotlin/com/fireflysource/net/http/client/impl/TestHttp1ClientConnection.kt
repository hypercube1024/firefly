package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpHeaderValue
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.tcp.aio.AioTcpClient
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestHttp1ClientConnection {

    //        @Test
    @Suppress("BlockingMethodInNonBlockingContext")
    fun test() = runBlocking {
        val tcpClient = AioTcpClient().enableSecureConnection()
        val conn = tcpClient.connect("www.baidu.com", 443).await()
        conn.startReading()
        val protocol = conn.onHandshakeComplete().await()
        println(protocol)

        val httpConn = Http1ClientConnection(conn)
        val request = AsyncHttpClientRequest()
        request.method = HttpMethod.GET.value
        request.uri = HttpURI("https://www.baidu.com/")
        request.httpFields.put(
            HttpHeader.USER_AGENT,
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36"
        )
        val response = httpConn.send(request).await()
        println("${response.status} ${response.reason}")
        println(response.httpFields)
        println()
        println(response.stringBody)
        conn.close()
        tcpClient.stop()
    }

    @Test
    fun testPrepareHttp1Headers() {
        val request = AsyncHttpClientRequest()
        request.method = HttpMethod.GET.value
        request.uri = HttpURI("https://www.fireflysource.com/")
        prepareHttp1Headers(request)

        assertTrue(request.httpFields.getValuesList(HttpHeader.HOST.value).isNotEmpty())
        assertEquals("www.fireflysource.com", request.httpFields[HttpHeader.HOST.value])
        assertEquals(HttpHeaderValue.KEEP_ALIVE.value, request.httpFields[HttpHeader.CONNECTION.value])
    }

    @Test
    fun testExistConnectionHeaders() {
        val request = AsyncHttpClientRequest()
        request.method = HttpMethod.GET.value
        request.uri = HttpURI("https://www.fireflysource.com/")
        request.httpFields.addCSV(HttpHeader.CONNECTION, HttpHeaderValue.UPGRADE.value, "HTTP2-Settings")
        prepareHttp1Headers(request)

        assertEquals(
            "${HttpHeaderValue.UPGRADE.value}, HTTP2-Settings, ${HttpHeaderValue.KEEP_ALIVE.value}",
            request.httpFields[HttpHeader.CONNECTION.value]
        )
    }
}