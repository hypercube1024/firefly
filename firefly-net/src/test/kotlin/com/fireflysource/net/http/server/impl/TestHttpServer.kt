package com.fireflysource.net.http.server.impl

import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.common.model.HttpStatus
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.CompletableFuture
import kotlin.system.measureTimeMillis

class TestHttpServer : AbstractHttpServerTestBase() {

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should visit router chain successfully.")
    fun testRouterChain(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        httpServer
            .router().get("/hello/:foo").handler { ctx ->
                val p = ctx.getPathParameter("foo")
                ctx.write("visit foo: $p |").next()
            }
            .router().get("/hello/*").handler { ctx ->
                val p = ctx.getPathParameter(0)
                ctx.write("visit pattern: $p |").next()
            }
            .router().method(HttpMethod.GET).pathRegex("/hello/([a-z]+)").handler { ctx ->
                val p = ctx.getPathParameterByRegexGroup(1)
                ctx.write("regex group: $p |").next()
            }
            .router().get("/hello/test").handler { ctx ->
                ctx.write("visit test ").next()
            }
            .router(99).path("*").handler { ctx ->
                ctx.end()
            }
            .listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("$schema://${address.hostName}:${address.port}/hello/bar").submit() }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals(
                "visit foo: bar |visit pattern: bar |regex group: bar |",
                response.stringBody
            )
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should enable and disable router successfully.")
    fun testEnableAndDisableRouter(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        httpServer
            .router().enable().get("/hello/:foo").handler { ctx ->
                val p = ctx.getPathParameter("foo")
                ctx.write("visit foo: $p ").next()
            }
            .router().disable().get("/hello/*").handler { ctx ->
                val p = ctx.getPathParameter(0)
                ctx.write("visit pattern: $p ").next()
            }
            .router().get("/hello/test").handler { ctx ->
                ctx.write("visit test ").next()
            }
            .router().path("*").handler { ctx -> ctx.end() }
            .listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("$schema://${address.hostName}:${address.port}/hello/test").submit() }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals(
                "visit foo: test visit test ",
                response.stringBody
            )
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should get and set attributes successfully.")
    fun testContextAttributes(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        httpServer
            .router().get("/hello/:foo").handler { ctx ->
                ctx.attributes["A"] = "TestA"
                ctx.next()
            }
            .router().get("/hello/:bar").handler { ctx ->
                ctx.attributes["B"] = "TestB"
                ctx.setAttribute("C", "TestC")
                ctx.setAttribute("D", "TestD")
                ctx.removeAttribute("D")
                ctx.removeAttribute("E")
                ctx.next()
            }
            .router().method("GET").path("/hello/*").handler { ctx ->
                ctx.write("${ctx.attributes["A"]} ${ctx.attributes["B"]} ${ctx.getAttribute("C")}").next()
            }
            .router().method(HttpMethod.GET).handler { ctx -> ctx.end() }
            .listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("$schema://${address.hostName}:${address.port}/hello/test").submit() }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals(
                "TestA TestB TestC",
                response.stringBody
            )
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should response 500 when the router does not commit.")
    fun testRouterNotCommit(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        httpServer.router().get("/hello").handler {
            Result.DONE
        }.listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("$schema://${address.hostName}:${address.port}/hello").submit() }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.status)
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should response 400 when the resource not found.")
    fun testResourceNotFound(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        httpServer.router().get("/hello").handler {
            Result.DONE
        }.listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("$schema://${address.hostName}:${address.port}/hellox").submit() }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.NOT_FOUND_404, response.status)
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }
}