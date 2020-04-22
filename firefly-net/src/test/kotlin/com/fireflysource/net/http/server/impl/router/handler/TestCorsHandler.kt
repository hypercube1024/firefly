package com.fireflysource.net.http.server.impl.router.handler

import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.impl.AbstractHttpServerTestBase
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.CompletableFuture
import kotlin.system.measureTimeMillis

class TestCorsHandler : AbstractHttpServerTestBase() {

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should allow request successfully.")
    fun testSimpleRequest(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        val corsConfig = CorsConfig("*.cors.test.com")
        httpServer
            .router().path("*").handler(CorsHandler(corsConfig))
            .router().get("/cors-simple-request/*").handler { it.end("success") }
            .listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map {
                    httpClient.get("$schema://${address.hostName}:${address.port}/cors-simple-request/$it")
                        .put(HttpHeader.ORIGIN, "simple.request.cors.test.com")
                        .submit()
                }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            Assertions.assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("success", response.stringBody)
            assertEquals("simple.request.cors.test.com", response.httpFields[HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN])
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should not allow simple request.")
    fun testNotAllowSimpleRequest(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        val corsConfig = CorsConfig("*.cors.test.com")
        httpServer
            .router().path("*").handler(CorsHandler(corsConfig))
            .router().get("/cors-simple-request/*").handler { it.end("success") }
            .listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map {
                    httpClient.get("$schema://${address.hostName}:${address.port}/cors-simple-request/$it")
                        .put(HttpHeader.ORIGIN, "www.fireflysource.com")
                        .submit()
                }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            Assertions.assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.FORBIDDEN_403, response.status)
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }
}