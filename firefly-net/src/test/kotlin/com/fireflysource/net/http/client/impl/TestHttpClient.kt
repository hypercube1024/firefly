package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.client.impl.content.provider.ByteBufferContentProvider
import com.fireflysource.net.http.common.model.Cookie
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.HttpServer
import com.fireflysource.net.http.server.HttpServerFactory
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class TestHttpClient {

    private lateinit var address: InetSocketAddress
    private lateinit var httpServer: HttpServer
    private val content = (1..50000).joinToString("") { it.toString() }

    @BeforeEach
    fun init() {
        address = InetSocketAddress("localhost", Random.nextInt(2000, 5000))
        httpServer = HttpServerFactory.create()
        httpServer
            .router().path("/testHttpClient").handler { ctx ->
                ctx.setCookies(listOf(Cookie("cookie1", "value1"), Cookie("cookie2", "value2")))
                    .put(HttpHeader.CONTENT_LENGTH, "14")
                    .write("test client ok")
                    .end()
            }
            .router().path("/testChunkedEncoding").handler { ctx ->
                ctx.end("test chunked encoding success")
            }
            .router().path("/testNoChunkedEncoding").handler { ctx ->
                ctx.put(HttpHeader.CONTENT_LENGTH, "32").end("test no chunked encoding success")
            }
            .timeout(120 * 1000)
            .listen(address)

    }

    @AfterEach
    fun destroy() {
        val time = measureTimeMillis {
            httpServer.stop()
        }
        println("shutdown time: $time ms")
    }

    @Test
    @DisplayName("should send the HTTP request no content successfully")
    fun testNoContent() = runBlocking {
        val httpClient = HttpClientFactory.create()
        val count = 100

        val time = measureTimeMillis {
            val futures =
                (1..count).map { httpClient.get("http://${address.hostName}:${address.port}/testHttpClient").submit() }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals(14L, response.contentLength)
            assertEquals("test client ok", response.stringBody)

            assertEquals(2, response.cookies.size)
            assertEquals("value1", response.cookies.filter { it.name == "cookie1" }.map { it.value }[0])
            assertEquals("value2", response.cookies.filter { it.name == "cookie2" }.map { it.value }[0])

        }

        val throughput = count / (time / 1000.00)
        println("success. $time ms, ${throughput.roundToLong()} qps")
        httpClient.stop()
    }

    @Test
    @DisplayName("should send the HTTP request with content using chucked encoding successfully")
    fun testContentWithChunkedEncoding() = runBlocking {
        val httpClient = HttpClientFactory.create()

        val data = ByteBuffer.wrap(content.toByteArray(StandardCharsets.UTF_8))
        println("data length: ${data.remaining()}")
        val response = httpClient
            .post("http://${address.hostName}:${address.port}/testChunkedEncoding")
            .contentProvider(MockChunkByteBufferContentProvider(data)).submit().await()

        assertEquals(HttpStatus.OK_200, response.status)
        assertEquals("test chunked encoding success", response.stringBody)

        httpClient.stop()
    }

    @Test
    @DisplayName("should send the HTTP request with content and content length successfully")
    fun testContentWithoutChunkedEncoding() = runBlocking {
        val httpClient = HttpClientFactory.create()

        (1..10).map {
            val data = ByteBuffer.wrap(content.toByteArray(StandardCharsets.UTF_8))
            val length = data.remaining()
            println("data length: $length")
            httpClient.post("http://${address.hostName}:${address.port}/testNoChunkedEncoding")
                .contentProvider(ByteBufferContentProvider(data))
                .submit()
        }.forEach {
            val response = it.await()
            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("test no chunked encoding success", response.stringBody)
            assertEquals(32, response.contentLength)
        }

        httpClient.stop()
    }

    class MockChunkByteBufferContentProvider(content: ByteBuffer) : ByteBufferContentProvider(content) {
        override fun length(): Long = -1
    }
}