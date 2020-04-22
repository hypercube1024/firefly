package com.fireflysource.net.http.server.impl.router.handler

import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.common.model.MimeTypes
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

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should response preflight request successfully.")
    fun testPreflightRequest(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        val corsConfig = CorsConfig("*.cors.test.com")
        httpServer
            .router().path("*").handler(CorsHandler(corsConfig))
            .router().post("/cors-data-request/*").handler { it.end("success") }
            .listen(address)


        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map {
                    httpClient.request(
                        HttpMethod.OPTIONS,
                        "$schema://${address.hostName}:${address.port}/cors-data-request/$it"
                    )
                        .put(HttpHeader.ORIGIN, "data.request.cors.test.com")
                        .put(HttpHeader.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.value)
                        .put(HttpHeader.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeader.CONTENT_TYPE.lowerCaseValue)
                        .submit()
                        .thenCompose { resp ->
                            if (resp.status == HttpStatus.NO_CONTENT_204) {
                                httpClient.post("$schema://${address.hostName}:${address.port}/cors-data-request/$it")
                                    .put(HttpHeader.ORIGIN, "data.request.cors.test.com")
                                    .put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.value)
                                    .body(
                                        """
                                        |{"id": 333}
                                    """.trimMargin()
                                    )
                                    .submit()
                            } else {
                                val future = CompletableFuture<HttpClientResponse>()
                                future.complete(resp)
                                future
                            }
                        }
                }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            Assertions.assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("success", response.stringBody)
            assertEquals("data.request.cors.test.com", response.httpFields[HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN])
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should block by preflight request when the headers do not allow to access.")
    fun testNotAllowHeader(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        val corsConfig = CorsConfig("*.cors.test.com")
        httpServer
            .router().path("*").handler(CorsHandler(corsConfig))
            .router().post("/cors-data-request/*").handler { it.end("success") }
            .listen(address)


        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map {
                    httpClient.request(
                        HttpMethod.OPTIONS,
                        "$schema://${address.hostName}:${address.port}/cors-data-request/$it"
                    )
                        .put(HttpHeader.ORIGIN, "data.request.cors.test.com")
                        .put(HttpHeader.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.value)
                        .put(HttpHeader.ACCESS_CONTROL_REQUEST_HEADERS, "x1-x2")
                        .submit()
                        .thenCompose { resp ->
                            if (resp.status == HttpStatus.NO_CONTENT_204) {
                                httpClient.post("$schema://${address.hostName}:${address.port}/cors-data-request/$it")
                                    .put(HttpHeader.ORIGIN, "data.request.cors.test.com")
                                    .put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.value)
                                    .body(
                                        """
                                        |{"id": 333}
                                    """.trimMargin()
                                    )
                                    .submit()
                            } else {
                                val future = CompletableFuture<HttpClientResponse>()
                                future.complete(resp)
                                future
                            }
                        }
                }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            Assertions.assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.BAD_REQUEST_400, response.status)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should block by preflight request when the methods do not allow to access.")
    fun testNotAllowMethod(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        val corsConfig = CorsConfig("*.cors.test.com")
        corsConfig.allowMethods = setOf(HttpMethod.GET.value)
        httpServer
            .router().path("*").handler(CorsHandler(corsConfig))
            .router().post("/cors-data-request/*").handler { it.end("success") }
            .listen(address)


        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map {
                    httpClient.request(
                        HttpMethod.OPTIONS,
                        "$schema://${address.hostName}:${address.port}/cors-data-request/$it"
                    )
                        .put(HttpHeader.ORIGIN, "data.request.cors.test.com")
                        .put(HttpHeader.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.value)
                        .put(HttpHeader.ACCESS_CONTROL_REQUEST_HEADERS, MimeTypes.Type.APPLICATION_JSON_UTF_8.value)
                        .submit()
                        .thenCompose { resp ->
                            if (resp.status == HttpStatus.NO_CONTENT_204) {
                                httpClient.post("$schema://${address.hostName}:${address.port}/cors-data-request/$it")
                                    .put(HttpHeader.ORIGIN, "data.request.cors.test.com")
                                    .put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON_UTF_8.value)
                                    .body(
                                        """
                                        |{"id": 333}
                                    """.trimMargin()
                                    )
                                    .submit()
                            } else {
                                val future = CompletableFuture<HttpClientResponse>()
                                future.complete(resp)
                                future
                            }
                        }
                }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            Assertions.assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.METHOD_NOT_ALLOWED_405, response.status)
        }

        finish(count, time, httpClient, httpServer)
    }
}