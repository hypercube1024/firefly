package com.fireflysource.net.http.server.impl

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.lifecycle.AbstractLifeCycle.stopAll
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.client.HttpClient
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.HttpServerContentProviderFactory.stringBody
import com.fireflysource.net.http.server.RoutingContext
import com.fireflysource.net.http.server.impl.content.provider.DefaultContentProvider
import com.fireflysource.net.tcp.TcpServer
import com.fireflysource.net.tcp.TcpServerFactory
import com.fireflysource.net.tcp.aio.ApplicationProtocol.HTTP1
import com.fireflysource.net.tcp.aio.ApplicationProtocol.HTTP2
import com.fireflysource.net.tcp.onAcceptAsync
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class TestHttpServerConnection {

    companion object {
        @JvmStatic
        fun testParametersProvider(): Stream<Arguments> {
            return Stream.of(
                arguments("http1", "http"),
                arguments("http1", "https"),
                arguments("http2", "https")
            )
        }
    }

    private lateinit var address: InetSocketAddress

    @BeforeEach
    fun init() {
        address = InetSocketAddress("localhost", Random.nextInt(20000, 40000))
    }

    @AfterEach
    fun destroy() {
        val stopTime = measureTimeMillis {
            stopAll()
        }
        println("stop success. $stopTime")
    }

    private fun createHttpServer(protocol: String, schema: String, listener: HttpServerConnection.Listener): TcpServer {
        val server = TcpServerFactory.create().timeout(120 * 1000).enableOutputBuffer()
        when (protocol) {
            "http1" -> server.supportedProtocols(listOf(HTTP1.value))
            "http2" -> server.supportedProtocols(listOf(HTTP2.value, HTTP1.value))
        }
        if (schema == "https") {
            server.enableSecureConnection()
        }
        return server.onAcceptAsync { connection ->
            println("accept connection. ${connection.id}")
            connection.beginHandshake().await()
            when (protocol) {
                "http1" -> {
                    val http1Connection = Http1ServerConnection(HttpConfig(), connection)
                    http1Connection.setListener(listener).begin()
                }
                "http2" -> {
                    val http2Connection = Http2ServerConnection(HttpConfig(), connection)
                    http2Connection.setListener(listener).begin()
                }
            }
        }.listen(address)
    }

    private fun finish(count: Int, time: Long, httpClient: HttpClient, httpServer: TcpServer) {
        try {
            val throughput = count / (time / 1000.00)
            println("success. $time ms, ${throughput.roundToLong()} qps")
            httpClient.stop()
            httpServer.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should receive request and response texts successfully.")
    fun test(protocol: String, schema: String): Unit = runBlocking {
        val count = 100

        val httpServer = createHttpServer(protocol, schema, object : HttpServerConnection.Listener.Adapter() {
            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                ctx.write(BufferUtils.toBuffer("response buffer.", StandardCharsets.UTF_8))
                val arr = arrayOf(
                    BufferUtils.toBuffer("array 1.", StandardCharsets.UTF_8),
                    BufferUtils.toBuffer("array 2.", StandardCharsets.UTF_8),
                    BufferUtils.toBuffer("array 3.", StandardCharsets.UTF_8)
                )
                val list = listOf(
                    BufferUtils.toBuffer("list 1.", StandardCharsets.UTF_8),
                    BufferUtils.toBuffer("list 2.", StandardCharsets.UTF_8),
                    BufferUtils.toBuffer("list 3.", StandardCharsets.UTF_8)
                )
                return ctx.write(arr, 0, arr.size)
                    .write(list, 1, 2)
                    .end("hello http1 server!")
            }

            override fun onException(ctx: RoutingContext?, exception: Throwable): CompletableFuture<Void> {
                exception.printStackTrace()
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("$schema://${address.hostName}:${address.port}/test-$it").submit() }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals(
                "response buffer.array 1.array 2.array 3.list 2.list 3.hello http1 server!",
                response.stringBody
            )
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should response texts with content length successfully.")
    fun testResponseContentLength(protocol: String, schema: String): Unit = runBlocking {
        val count = 100

        val httpServer = createHttpServer(protocol, schema, object : HttpServerConnection.Listener.Adapter() {
            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                val buffer = BufferUtils.toBuffer("response text with content length.", StandardCharsets.UTF_8)
                ctx.put(HttpHeader.CONTENT_LENGTH, buffer.remaining().toString())
                return ctx.write(buffer).end()
            }

            override fun onException(ctx: RoutingContext?, exception: Throwable): CompletableFuture<Void> {
                exception.printStackTrace()
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("$schema://${address.hostName}:${address.port}/length-$it").submit() }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("response text with content length.", response.stringBody)
            assertEquals(34L, response.contentLength)
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should receive query strings and form inputs successfully.")
    fun testQueryStringsAndFormInputs(protocol: String, schema: String): Unit = runBlocking {
        val count = 100

        val httpServer = createHttpServer(protocol, schema, object : HttpServerConnection.Listener.Adapter() {
            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                val query = ctx.getQueryString("key1")
                val queryList = ctx.getQueryStrings("list1")
                val querySize = ctx.queryStrings.size
                val message = ctx.getFormInput("key1")
                val formList = ctx.getFormInputs("list1")
                val formSize = ctx.formInputs.size
                val method = ctx.method
                return ctx.write(method).write(", ")
                    .write(query).write(queryList.toString()).write(", size: $querySize")
                    .write(", ")
                    .write(message).write(formList.toString()).write(", size: $formSize")
                    .end()
            }

            override fun onException(ctx: RoutingContext?, exception: Throwable): CompletableFuture<Void> {
                exception.printStackTrace()
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.post("$schema://${address.hostName}:${address.port}/query-form-$it")
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
            assertEquals("POST, query[q1, q2, q3], size: 2, message[v1, v2, v3, v4, v5], size: 2", response.stringBody)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should response default html successfully.")
    fun testContentProvider(protocol: String, schema: String): Unit = runBlocking {
        val count = 100

        val httpServer = createHttpServer(protocol, schema, object : HttpServerConnection.Listener.Adapter() {
            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return ctx.setStatus(HttpStatus.NOT_FOUND_404).setReason("Just so so")
                    .setHttpVersion(HttpVersion.HTTP_1_1)
                    .contentProvider(DefaultContentProvider(HttpStatus.NOT_FOUND_404, null, ctx))
                    .end()
            }

            override fun onException(ctx: RoutingContext?, exception: Throwable): CompletableFuture<Void> {
                exception.printStackTrace()
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("$schema://${address.hostName}:${address.port}/not-found-$it").submit() }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()

            assertEquals(HttpStatus.NOT_FOUND_404, response.status)
            if (protocol == "http1") {
                assertEquals("Just so so", response.reason)
            }
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should response trailer successfully.")
    fun testTrailer(protocol: String, schema: String): Unit = runBlocking {
        val count = 100

        val httpServer = createHttpServer(protocol, schema, object : HttpServerConnection.Listener.Adapter() {
            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return ctx.addCSV(HttpHeader.TRAILER, "t1", "t2", "t3")
                    .setTrailerSupplier {
                        val fields = HttpFields()
                        fields.put("t1", "trailer1")
                        fields.put("t2", "trailer2")
                        fields.put("t3", "trailer3")
                        fields
                    }
                    .write("response text success.")
                    .write("trailer.")
                    .end()
            }

            override fun onException(ctx: RoutingContext?, exception: Throwable): CompletableFuture<Void> {
                exception.printStackTrace()
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("$schema://${address.hostName}:${address.port}/trailer-$it").submit() }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()
            println(response)
            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("t1, t2, t3", response.httpFields[HttpHeader.TRAILER])
            assertEquals("response text success.trailer.", response.stringBody)
            assertEquals("trailer1", response.trailerSupplier.get()["t1"])
            assertEquals("trailer2", response.trailerSupplier.get()["t2"])
            assertEquals("trailer3", response.trailerSupplier.get()["t3"])

        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should redirect successfully.")
    fun testRedirect(protocol: String, schema: String): Unit = runBlocking {
        val count = 100

        val httpServer = createHttpServer(protocol, schema, object : HttpServerConnection.Listener.Adapter() {
            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return ctx.redirect("http://${address.hostName}:${address.port}/r0")
            }

            override fun onException(ctx: RoutingContext?, exception: Throwable): CompletableFuture<Void> {
                exception.printStackTrace()
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("$schema://${address.hostName}:${address.port}/redirect-$it").submit() }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()
            println(response)
            assertEquals(HttpStatus.FOUND_302, response.status)
            assertEquals("http://${address.hostName}:${address.port}/r0", response.httpFields[HttpHeader.LOCATION])
        }

        finish(count, time, httpClient, httpServer)
    }


    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should get cookies and set cookies successfully.")
    fun testCookies(protocol: String, schema: String): Unit = runBlocking {
        val count = 100

        val httpServer = createHttpServer(protocol, schema, object : HttpServerConnection.Listener.Adapter() {
            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                ctx.cookies = listOf(
                    Cookie("s1", "v1"),
                    Cookie("s2", "v2"),
                    Cookie("s3", ctx.cookies[0].value),
                    Cookie("s4", ctx.cookies[1].value)
                )
                return ctx.end("receive ${ctx.stringBody} ok.")
            }

            override fun onException(ctx: RoutingContext?, exception: Throwable): CompletableFuture<Void> {
                exception.printStackTrace()
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.post("$schema://${address.hostName}:${address.port}/cookies-$it")
                    .body("cookies c1, c2.")
                    .cookies(listOf(Cookie("c1", "client1"), Cookie("c2", "client2")))
                    .submit()
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()
            println(response)
            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals(4, response.cookies.size)
            assertEquals("receive cookies c1, c2. ok.", response.stringBody)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should receive gbk content successfully.")
    fun testGBK(protocol: String, schema: String): Unit = runBlocking {
        val count = 100
        val charset = Charset.forName("GBK")

        val httpServer = createHttpServer(protocol, schema, object : HttpServerConnection.Listener.Adapter() {
            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                val content = ctx.getStringBody(charset)
                return ctx.contentProvider(stringBody("收到：${content}。长度：${ctx.contentLength}", charset)).end()
            }

            override fun onException(ctx: RoutingContext?, exception: Throwable): CompletableFuture<Void> {
                exception.printStackTrace()
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.post("$schema://${address.hostName}:${address.port}/gbk-$it")
                    .body("发射！！Oooo", charset)
                    .submit()
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()
            println(response.getStringBody(charset))
            assertEquals(HttpStatus.OK_200, response.status)
//            assertEquals("收到：发射！！Oooo。长度：12", response.getStringBody(charset))
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should accept 100 continue.")
    fun testAccept100Continue(protocol: String, schema: String): Unit = runBlocking {
        val count = 100

        val httpServer = createHttpServer(protocol, schema, object : HttpServerConnection.Listener.Adapter() {
            override fun onHeaderComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return if (ctx.expect100Continue()) ctx.response100Continue() else Result.DONE
            }

            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return ctx.end("receive ${ctx.stringBody} OK")
            }

            override fun onException(ctx: RoutingContext?, exception: Throwable): CompletableFuture<Void> {
                exception.printStackTrace()
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.post("$schema://${address.hostName}:${address.port}/100-continue-$it")
                    .put(HttpHeader.EXPECT, HttpHeaderValue.CONTINUE.value)
                    .body("100 continue content")
                    .submit()
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()
            println(response)
            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("receive 100 continue content OK", response.stringBody)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should not accept 100 continue and receive the error status successfully.")
    fun testNotAccept100Continue(protocol: String, schema: String): Unit = runBlocking {
        val count = 100

        val httpServer = createHttpServer(protocol, schema, object : HttpServerConnection.Listener.Adapter() {
            override fun onHeaderComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return ctx.setStatus(HttpStatus.PAYLOAD_TOO_LARGE_413).end("Content too large")
            }

            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return Result.DONE
            }

            override fun onException(ctx: RoutingContext?, exception: Throwable): CompletableFuture<Void> {
                println("server exception. ${exception.message}")
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.post("$schema://${address.hostName}:${address.port}/100-continue-$it")
                    .put(HttpHeader.EXPECT, HttpHeaderValue.CONTINUE.value)
                    .body("100 continue content")
                    .submit()
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()
            println(response)
            assertEquals(HttpStatus.PAYLOAD_TOO_LARGE_413, response.status)
            assertEquals("Content too large", response.stringBody)
        }

        finish(count, time, httpClient, httpServer)
    }

    @Test
    @DisplayName("should close connection successfully.")
    fun testCloseConnection(): Unit = runBlocking {
        val count = 20

        val httpServer = createHttpServer("http1", "http", object : HttpServerConnection.Listener.Adapter() {
            override fun onHeaderComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return Result.DONE
            }

            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return ctx.put(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE.value)
                    .contentProvider(stringBody("Close connection success", StandardCharsets.UTF_8))
                    .end()
            }

            override fun onException(ctx: RoutingContext?, exception: Throwable): CompletableFuture<Void> {
                println("server exception. ${exception.message}")
                return Result.DONE
            }
        })

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.get("http://${address.hostName}:${address.port}/close-$it")
                    .put(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE.value)
                    .submit()
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)
            val response = futures[0].await()

            println(response)
            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("Close connection success", response.stringBody)
        }

        finish(count, time, httpClient, httpServer)
    }

}