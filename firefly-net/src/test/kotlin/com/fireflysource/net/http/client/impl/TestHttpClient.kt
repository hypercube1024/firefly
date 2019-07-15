package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.common.model.HttpStatus
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import kotlin.random.Random

class TestHttpClient {

    private lateinit var address: InetSocketAddress
    private lateinit var httpServer: HttpServer

    @BeforeEach
    fun init() {
        address = InetSocketAddress("localhost", Random.nextInt(2000, 5000))
        httpServer = HttpServer.create(address, 1024)

        httpServer.createContext("/testHttpClient") { exg ->
            val body = "test client ok".toByteArray(StandardCharsets.UTF_8)
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
        val httpClient = HttpClientFactory.create()

        repeat(5) {
            val response = httpClient.get("http://${address.hostName}:${address.port}/testHttpClient").submit().await()
            println("${response.status} ${response.reason}")
            println(response.httpFields)
            println(response.stringBody)
            println()

            Assertions.assertEquals(HttpStatus.OK_200, response.status)
            Assertions.assertEquals(14L, response.contentLength)
            Assertions.assertEquals("test client ok", response.stringBody)
        }

        httpClient.stop()
    }
}