package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.common.model.HttpURI
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.random.Random

class TestAsyncHttpClientConnectionManager {

    private lateinit var address: InetSocketAddress
    private lateinit var httpServer: HttpServer

    @BeforeEach
    fun init() {
        address = InetSocketAddress("localhost", Random.nextInt(2000, 5000))
        httpServer = HttpServer.create(address, 1024)

        httpServer.createContext("/test1") { exg ->
            val body = "test ok".toByteArray(StandardCharsets.UTF_8)
            exg.sendResponseHeaders(200, body.size.toLong())
            exg.responseBody.use { out -> out.write(body) }
            exg.close()
        }
        httpServer.start()
    }

    @AfterEach
    fun destroy() {
        httpServer.stop(5)
    }

    @Test
    fun test() = runBlocking {
        val client = AsyncHttpClientConnectionManager()

        repeat(5) {
            val request = AsyncHttpClientRequest()
            request.method = HttpMethod.GET.value
            request.uri = HttpURI(URL("http://${address.hostName}:${address.port}/test1").toURI())

            val response = client.send(request).await()
            println("${response.status} ${response.reason}")
            println(response.httpFields)
            println()
            println(response.stringBody)

            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals(7L, response.contentLength)
            assertEquals("test ok", response.stringBody)
        }

        client.stop()
    }
}