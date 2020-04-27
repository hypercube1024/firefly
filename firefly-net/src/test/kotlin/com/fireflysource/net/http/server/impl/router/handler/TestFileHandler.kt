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
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.system.measureTimeMillis

class TestFileHandler : AbstractHttpServerTestBase() {

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should get file successfully.")
    fun testFile(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        val path = Optional.ofNullable(FileHandler::class.java.classLoader.getResource("files"))
            .map { it.toURI() }
            .map { Paths.get(it) }
            .map { it.toString() }
            .orElse("")
        val fileConfig = FileConfig(path)

        httpServer
            .router().paths(listOf("/favicon.ico", "/*.html", "/*.txt")).handler(FileHandler(fileConfig))
            .listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map {
                    httpClient.get("$schema://${address.hostName}:${address.port}/poem.html")
                        .submit()
                }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            Assertions.assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("text/html", response.httpFields[HttpHeader.CONTENT_TYPE])
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should do not find the file.")
    fun testFileNotFound(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        val path = Optional.ofNullable(FileHandler::class.java.classLoader.getResource("files"))
            .map { it.toURI() }
            .map { Paths.get(it) }
            .map { it.toString() }
            .orElse("")
        val fileConfig = FileConfig(path)

        httpServer
            .router().paths(listOf("/favicon.ico", "/*.html", "/*.txt")).handler(FileHandler(fileConfig))
            .listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map {
                    httpClient.get("$schema://${address.hostName}:${address.port}/poem-$it.html")
                        .submit()
                }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            Assertions.assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.status)
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should get partial file successfully.")
    fun testPartialFile(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        val path = Optional.ofNullable(FileHandler::class.java.classLoader.getResource("files"))
            .map { it.toURI() }
            .map { Paths.get(it) }
            .map { it.toString() }
            .orElse("")
        val fileConfig = FileConfig(path)

        httpServer
            .router().paths(listOf("/favicon.ico", "/*.html", "/*.txt")).handler(FileHandler(fileConfig))
            .listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map {
                    httpClient.get("$schema://${address.hostName}:${address.port}/poem.html")
                        .put(HttpHeader.RANGE, "bytes=5-10")
                        .submit()
                }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            Assertions.assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.PARTIAL_CONTENT_206, response.status)
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should get partial file unsuccessfully.")
    fun testRangeNotSatisfiable(protocol: String, schema: String): Unit = runBlocking {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        val path = Optional.ofNullable(FileHandler::class.java.classLoader.getResource("files"))
            .map { it.toURI() }
            .map { Paths.get(it) }
            .map { it.toString() }
            .orElse("")
        val fileConfig = FileConfig(path)

        httpServer
            .router().paths(listOf("/favicon.ico", "/*.html", "/*.txt")).handler(FileHandler(fileConfig))
            .listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map {
                    httpClient.get("$schema://${address.hostName}:${address.port}/poem.html")
                        .put(HttpHeader.RANGE, "bytes=1000000000000000000-")
                        .submit()
                }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            Assertions.assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.RANGE_NOT_SATISFIABLE_416, response.status)
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }
}