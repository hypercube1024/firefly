package com.fireflysource.net.http.client.impl

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.useAwait
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.client.impl.content.provider.ByteBufferContentProvider
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.exception.MissingRemoteHostException
import com.fireflysource.net.http.common.model.ContentEncoding
import com.fireflysource.net.http.common.model.Cookie
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.HttpServer
import com.fireflysource.net.http.server.HttpServerFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
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
            .router().get("/testCompressedContent").handler { ctx ->
                ctx.put(HttpHeader.CONTENT_ENCODING, ContentEncoding.GZIP.value)
                    .write("测试压缩内容：")
                    .write(
                        mutableListOf(
                            BufferUtils.toBuffer("跳过", StandardCharsets.UTF_8),
                            BufferUtils.toBuffer("跳过", StandardCharsets.UTF_8),
                            BufferUtils.toBuffer("今天，", StandardCharsets.UTF_8),
                            BufferUtils.toBuffer("非常愉快的", StandardCharsets.UTF_8),
                            BufferUtils.toBuffer("搞定了这个功能。", StandardCharsets.UTF_8)
                        ), 2, 3
                    )
                    .end()
            }
            .router().get("/echo0").handler { it.end("ok") }
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

    @Test
    @DisplayName("should get compressed content successfully")
    fun testCompressedContent() = runBlocking {
        val httpClient = HttpClientFactory.create()
        val response =
            httpClient.get("http://${address.hostName}:${address.port}/testCompressedContent").submit().await()

        assertEquals(HttpStatus.OK_200, response.status)
        assertEquals("测试压缩内容：今天，非常愉快的搞定了这个功能。", response.stringBody)
        println(response)

        httpClient.stop()
    }

    @Test
    @DisplayName("should retry to send request successfully")
    fun testConnectionTimeoutRetry() = runBlocking {
        val server2 = HttpServerFactory.create()
        val addr = InetSocketAddress("localhost", Random.nextInt(12000, 15000))
        server2.router().get("/timeout/echo")
            .handler {
                val cmd = it.getQueryString("cmd")
                it.end("test timeout $cmd")
            }
            .timeout(1)
            .listen(addr)

        val config = HttpConfig()
        config.connectionPoolSize = 1
        val httpClient = HttpClientFactory.create(config)

        suspend fun echo() {
            val url = "http://${addr.hostName}:${addr.port}/timeout/echo"
            val response = httpClient.get(url).addQueryString("cmd", "xx").submit().await()
            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("test timeout xx", response.stringBody)
            println(response)
        }

        echo()
        delay(2000)
        echo()

        httpClient.stop()
        server2.stop()
        Unit
    }

    @Test
    @DisplayName("should create HTTP client connection and send request successfully")
    fun testCreateHttpClientConnection() = runBlocking {
        val httpClient = HttpClientFactory.create()
        val uri = "http://${address.hostName}:${address.port}"
        val connection = httpClient.createHttpClientConnection(uri).await()

        connection.useAwait {
            val builder = httpClient.get("/echo0")
            val response = connection.send(builder.httpClientRequest).await()
            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("ok", response.stringBody)
            println(response)

            val builder1 = httpClient.get("/echo0")
            val response1 = connection.send(builder1.httpClientRequest).await()
            assertEquals(HttpStatus.OK_200, response1.status)
            assertEquals("ok", response1.stringBody)
            println(response1)
        }

        httpClient.stop()
    }

    @Test
    @DisplayName("should send request failure when the host does not set")
    fun testNoHostException() {
        val httpClient = HttpClientFactory.create()
        assertThrows(MissingRemoteHostException::class.java) {
            httpClient.get("/echo0").submit()
        }
    }

    class MockChunkByteBufferContentProvider(content: ByteBuffer) : ByteBufferContentProvider(content) {
        override fun length(): Long = -1
    }
}