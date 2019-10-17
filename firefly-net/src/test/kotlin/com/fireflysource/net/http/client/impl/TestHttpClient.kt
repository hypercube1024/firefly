package com.fireflysource.net.http.client.impl

import com.fireflysource.common.io.IO
import com.fireflysource.common.lifecycle.AbstractLifeCycle.stopAll
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.client.impl.content.provider.ByteBufferContentProvider
import com.fireflysource.net.http.common.codec.CookieGenerator
import com.fireflysource.net.http.common.model.Cookie
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpStatus
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
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
        httpServer = HttpServer.create(address, 1024)

        httpServer.createContext("/testHttpClient") { exg ->
            exg.responseHeaders.add(
                HttpHeader.SET_COOKIE.value,
                CookieGenerator.generateSetCookie(Cookie("cookie1", "value1"))
            )
            exg.responseHeaders.add(
                HttpHeader.SET_COOKIE.value,
                CookieGenerator.generateSetCookie(Cookie("cookie2", "value2"))
            )
            val body = "test client ok".toByteArray(StandardCharsets.UTF_8)
            exg.sendResponseHeaders(200, body.size.toLong())
            exg.responseBody.use { out -> out.write(body) }
            exg.close()
        }

        httpServer.createContext("/testChunkedEncoding") { exg ->
            val requestBody = exg.requestBody.use { IO.toString(it, StandardCharsets.UTF_8) }
            assertEquals(content, requestBody)

            val body = "test chunked encoding success".toByteArray(StandardCharsets.UTF_8)
            exg.sendResponseHeaders(200, body.size.toLong())
            exg.responseBody.use { out -> out.write(body) }
            exg.close()
        }

        httpServer.createContext("/testNoChunkedEncoding") { exg ->
            val requestBody = exg.requestBody.use { IO.toString(it, StandardCharsets.UTF_8) }
            assertEquals(content, requestBody)

            val body = "test no chunked encoding success".toByteArray(StandardCharsets.UTF_8)
            exg.sendResponseHeaders(200, body.size.toLong())
            exg.responseBody.use { out -> out.write(body) }
            exg.close()
        }
        httpServer.start()
    }

    @AfterEach
    fun destroy() {
        val time = measureTimeMillis {
            httpServer.stop(2)
            stopAll()
        }
        println("shutdown time: $time ms")
    }

    @Test
    fun testNoContent() = runBlocking {
        val httpClient = HttpClientFactory.create()
        val count = 1_000

        val time = measureTimeMillis {
            (1..count).map { httpClient.get("http://${address.hostName}:${address.port}/testHttpClient").submit() }
                .forEach { future ->
                    val response = future.await()

                    assertEquals(HttpStatus.OK_200, response.status)
                    assertEquals(14L, response.contentLength)
                    assertEquals("test client ok", response.stringBody)

                    assertEquals(2, response.cookies.size)
                    assertEquals("value1", response.cookies.filter { it.name == "cookie1" }.map { it.value }[0])
                    assertEquals("value2", response.cookies.filter { it.name == "cookie2" }.map { it.value }[0])
                }
        }

        val throughput = count / (time / 1000.00)
        println("success. $time ms, ${throughput.roundToLong()} qps")
    }

    @Test
    fun testContentWithChunkedEncoding() = runBlocking {
        val data = ByteBuffer.wrap(content.toByteArray(StandardCharsets.UTF_8))
        println("data length: ${data.remaining()}")

        val httpClient = HttpClientFactory.create()
        val response = httpClient
            .post("http://${address.hostName}:${address.port}/testChunkedEncoding")
            .contentProvider(object : ByteBufferContentProvider(data) {

                override fun length(): Long = -1

                override fun isOpen(): Boolean = buffer.hasRemaining()

            }).submit().await()

        assertEquals(HttpStatus.OK_200, response.status)
        assertEquals("test chunked encoding success", response.stringBody)
    }

    @Test
    fun testContentWithoutChunkedEncoding() = runBlocking {
        val data = ByteBuffer.wrap(content.toByteArray(StandardCharsets.UTF_8))
        val length = data.remaining()
        println("data length: $length")

        val httpClient = HttpClientFactory.create()
        val response = httpClient
            .post("http://${address.hostName}:${address.port}/testNoChunkedEncoding")
            .contentProvider(object : ByteBufferContentProvider(data) {

                override fun length(): Long = length.toLong()

                override fun isOpen(): Boolean = buffer.hasRemaining()

            }).submit().await()

        assertEquals(HttpStatus.OK_200, response.status)
        assertEquals("test no chunked encoding success", response.stringBody)
    }
}