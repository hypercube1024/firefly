package com.fireflysource.net.http.server.impl

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.lifecycle.AbstractLifeCycle.stopAll
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.RoutingContext
import com.fireflysource.net.tcp.TcpServerFactory
import com.fireflysource.net.tcp.onAcceptAsync
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class TestHttp1ServerConnection {

    private lateinit var address: InetSocketAddress

    @BeforeEach
    fun init() {
        address = InetSocketAddress("localhost", Random.nextInt(2000, 5000))
    }

    @AfterEach
    fun destroy() {
        val stopTime = measureTimeMillis {
            stopAll()
        }
        println("stop success. $stopTime")
    }

    @Test
    @DisplayName("should receive request and response texts successfully.")
    fun test(): Unit = runBlocking {
        val count = 100

        createHttpServer(object : HttpServerConnection.Listener.Adapter() {
            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                ctx.write(BufferUtils.toBuffer("response buffer.", StandardCharsets.UTF_8))
                val arr = arrayOf(
                    BufferUtils.toBuffer("response array 1.", StandardCharsets.UTF_8),
                    BufferUtils.toBuffer("response array 2.", StandardCharsets.UTF_8),
                    BufferUtils.toBuffer("response array 3.", StandardCharsets.UTF_8)
                )
                return ctx.write(arr, 0, arr.size).end("hello http1 server!")
            }

            override fun onException(ctx: RoutingContext?, exception: Exception): CompletableFuture<Void> {
                exception.printStackTrace()
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("http://${address.hostName}:${address.port}/test-$it").submit() }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals(
                "response buffer.response array 1.response array 2.response array 3.hello http1 server!",
                response.stringBody
            )
            println(response)
        }

        val throughput = count / (time / 1000.00)
        println("success. $time ms, ${throughput.roundToLong()} qps")
    }

    @Test
    @DisplayName("should response texts with content length successfully.")
    fun testResponseContentLength(): Unit = runBlocking {
        val count = 100

        createHttpServer(object : HttpServerConnection.Listener.Adapter() {
            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                val buffer = BufferUtils.toBuffer("response text with content length.", StandardCharsets.UTF_8)
                ctx.put(HttpHeader.CONTENT_LENGTH, buffer.remaining().toString())
                return ctx.write(buffer).end()
            }

            override fun onException(ctx: RoutingContext?, exception: Exception): CompletableFuture<Void> {
                exception.printStackTrace()
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("http://${address.hostName}:${address.port}/length-$it").submit() }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("response text with content length.", response.stringBody)
            assertEquals(34L, response.contentLength)
            println(response)
        }

        val throughput = count / (time / 1000.00)
        println("success. $time ms, ${throughput.roundToLong()} qps")
    }

    @Test
    @DisplayName("should receive query strings and form inputs successfully.")
    fun testQueryStringsAndFormInputs(): Unit = runBlocking {
        val count = 100

        createHttpServer(object : HttpServerConnection.Listener.Adapter() {
            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                val query = ctx.getQueryString("key1")
                val queryList = ctx.getQueryStrings("list1")
                val message = ctx.getFormInput("key1")
                val formList = ctx.getFormInputs("list1")
                val method = ctx.method
                return ctx.write(method).write(", ").write(query).write(queryList.toString()).write(", ")
                    .write(message).write(formList.toString())
                    .end()
            }

            override fun onException(ctx: RoutingContext?, exception: Exception): CompletableFuture<Void> {
                exception.printStackTrace()
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.post("http://${address.hostName}:${address.port}/query-form-$it")
                    .addQueryString("key1", "query")
                    .addQueryStrings("list1", listOf("q1", "q2", "q3"))
                    .addFormInput("key1", "message")
                    .addFormInputs("list1", listOf("v1", "v2", "v3", "v4", "v5"))
                    .submit()
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()
            println(response)
            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("POST, query[q1, q2, q3], message[v1, v2, v3, v4, v5]", response.stringBody)
        }

        val throughput = count / (time / 1000.00)
        println("success. $time ms, ${throughput.roundToLong()} qps")
    }

    private fun createHttpServer(listener: HttpServerConnection.Listener) {
        val server = TcpServerFactory.create().timeout(120 * 1000)
        server.onAcceptAsync { connection ->
            println("accept connection. ${connection.id}")
            connection.beginHandshake().await()
            val http1ServerConnection = Http1ServerConnection(HttpConfig(), connection)
            http1ServerConnection.setListener(listener).begin()
        }.listen(address)
    }

}