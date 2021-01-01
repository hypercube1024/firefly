package com.fireflysource.net.http.server.impl

import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.client.HttpClientContentProviderFactory
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.client.impl.content.provider.ByteBufferContentProvider
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.server.HttpServerContentProviderFactory.stringBody
import com.fireflysource.net.http.server.impl.content.provider.DefaultContentProvider
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.system.measureTimeMillis

class TestHttpServerConnection : AbstractHttpServerTestBase() {

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should receive request and response texts successfully.")
    fun test(protocol: String, schema: String): Unit = runBlocking {
        val count = 100

        val httpServer = createHttpServer(protocol, schema)
        httpServer.router().get("/test-*").handler { ctx ->
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
            ctx.write(arr, 0, arr.size)
                .write(list, 1, 2)
                .end("hello http1 server!")
        }.listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("$schema://${address.hostName}:${address.port}/test-$it").submit() }
            futures.forEach { f -> f.exceptionallyAccept { println(it.message) } }
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

        val httpServer = createHttpServer(protocol, schema)
        httpServer.router().get("/length-*").handler { ctx ->
            val buffer = BufferUtils.toBuffer("response text with content length.", StandardCharsets.UTF_8)
            ctx.put(HttpHeader.CONTENT_LENGTH, buffer.remaining().toString())
            ctx.write(buffer).end()
        }.listen(address)

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

        val httpServer = createHttpServer(protocol, schema)
        httpServer.router().post("/query-form-*").handler { ctx ->
            val query = ctx.getQueryString("key1")
            val queryList = ctx.getQueryStrings("list1")
            val querySize = ctx.queryStrings.size
            val message = ctx.getFormInput("key1")
            val formList = ctx.getFormInputs("list1")
            val formSize = ctx.formInputs.size
            val method = ctx.method
            ctx.write(method).write(", ")
                .write(query).write(queryList.toString()).write(", size: $querySize")
                .write(", ")
                .write(message).write(formList.toString()).write(", size: $formSize")
                .end()
        }.listen(address)

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

        val httpServer = createHttpServer(protocol, schema)
        httpServer.router().get("/not-found-*").handler { ctx ->
            ctx.setStatus(HttpStatus.NOT_FOUND_404).setReason("Just so so")
                .setHttpVersion(HttpVersion.HTTP_1_1)
                .contentProvider(DefaultContentProvider(HttpStatus.NOT_FOUND_404, null, ctx))
                .end()
        }.listen(address)

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

        val httpServer = createHttpServer(protocol, schema)
        httpServer.router().get("/trailer-*").handler { ctx ->
            ctx.addCSV(HttpHeader.TRAILER, "t1", "t2", "t3")
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
        }.listen(address)

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

        val httpServer = createHttpServer(protocol, schema)
        httpServer.router().get("/redirect-*").handler { ctx ->
            ctx.redirect("http://${address.hostName}:${address.port}/r0")
        }.listen(address)

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

        val httpServer = createHttpServer(protocol, schema)
        httpServer.router().post("/cookies-*").handler { ctx ->
            ctx.cookies = listOf(
                Cookie("s1", "v1"),
                Cookie("s2", "v2"),
                Cookie("s3", ctx.cookies[0].value),
                Cookie("s4", ctx.cookies[1].value)
            )
            ctx.end("receive ${ctx.stringBody} ok.")
        }.listen(address)

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

        val httpServer = createHttpServer(protocol, schema)
        httpServer.router().post("/gbk-*").handler { ctx ->
            val content = ctx.getStringBody(charset)
            ctx.contentProvider(stringBody("收到：${content}。长度：${ctx.contentLength}", charset)).end()
        }.listen(address)

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

        val httpServer = createHttpServer(protocol, schema)
        httpServer.router().post("/100-continue-*").handler { ctx ->
            ctx.end("receive ${ctx.stringBody} OK")
        }.listen(address)

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

        val httpServer = createHttpServer(protocol, schema)
        httpServer
            .onHeaderComplete { ctx ->
                ctx.setStatus(HttpStatus.PAYLOAD_TOO_LARGE_413).end("Content too large")
            }
            .router().post("/100-continue-*").handler { ctx ->
                if (ctx.response.isCommitted) Result.DONE
                else ctx.end("receive ${ctx.stringBody} OK")
            }
            .listen(address)

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
    @DisplayName("should close client connection successfully.")
    fun testCloseClientConnection(): Unit = runBlocking {
        val count = 30

        val httpServer = createHttpServer("http1", "http")
        httpServer.router().get("/close-*").handler { ctx ->
            ctx.put(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE.value)
                .contentProvider(stringBody("Close connection success", StandardCharsets.UTF_8))
                .end()
        }.listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.get("http://${address.hostName}:${address.port}/close-$it")
                    .put(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE.value)
                    .submit()
            }

            try {
                CompletableFuture.allOf(*futures.toTypedArray()).await()
                val allDone = futures.all { it.isDone }
                assertTrue(allDone)
                val response = futures[0].await()

                println(response)
                assertEquals(HttpStatus.OK_200, response.status)
                assertEquals("Close connection success", response.stringBody)
            } catch (e: Exception) {
                println(e.message)
            }
        }

        finish(count, time, httpClient, httpServer)
    }

    @Test
    @DisplayName("should close server connection successfully.")
    fun testCloseServerConnection(): Unit = runBlocking {
        val count = 30

        val httpServer = createHttpServer("http1", "http")
        httpServer.router().get("/close-*").handler { ctx ->
            ctx.put(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE.value)
                .contentProvider(stringBody("Close connection success", StandardCharsets.UTF_8))
                .end()
        }.listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.get("http://${address.hostName}:${address.port}/close-$it").submit()
            }

            try {
                CompletableFuture.allOf(*futures.toTypedArray()).await()
                val allDone = futures.all { it.isDone }
                assertTrue(allDone)
                val response = futures[0].await()

                println(response)
                assertEquals(HttpStatus.OK_200, response.status)
                assertEquals("Close connection success", response.stringBody)
            } catch (e: Exception) {
                println(e.message)
            }
        }

        finish(count, time, httpClient, httpServer)
    }

    @Test
    @DisplayName("should upgrade http2 protocol successfully.")
    fun testUpgradeHttp2(): Unit = runBlocking {
        val count = 30

        val httpServer = createHttpServer("http1", "http")
        httpServer.router().get("/upgrade-http2-*").handler { ctx ->
            ctx.end("Upgrade http2 success")
        }.listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.get("http://${address.hostName}:${address.port}/upgrade-http2-$it").upgradeHttp2().submit()
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)
            val response = futures[0].await()

            println(response)
            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("Upgrade http2 success", response.stringBody)
        }

        finish(count, time, httpClient, httpServer)
    }

    @Test
    @DisplayName("should trigger window update successfully.")
    fun testBufferedWindowUpdate(): Unit = runBlocking {
        val count = 1
        val content = (1..30_000_000).joinToString("") { "a" }
        val httpConfig = HttpConfig()
        httpConfig.initialSessionRecvWindow = HttpConfig.DEFAULT_WINDOW_SIZE
        httpConfig.initialStreamRecvWindow = HttpConfig.DEFAULT_WINDOW_SIZE

        val httpServer = createHttpServer("http2", "https", httpConfig)
        httpServer.router().post("/big-data-http2-*").handler { ctx ->
            ctx.end("Received data success. length: ${ctx.stringBody.length}")
        }.listen(address)

        val httpClient = HttpClientFactory.create(httpConfig)
        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.post("https://${address.hostName}:${address.port}/big-data-http2-$it")
                    .contentProvider(ByteBufferContentProvider(ByteBuffer.wrap(content.toByteArray(StandardCharsets.UTF_8))))
                    .submit()
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)
            val response = futures[0].await()

            println(response)
            assertEquals(HttpStatus.OK_200, response.status)
            assertTrue(response.stringBody.contains("Received data success."))
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should receive multi part content successfully.")
    fun testMultiPartContent(protocol: String, schema: String): Unit = runBlocking {
        val count = 100

        val httpServer = createHttpServer(protocol, schema)
        httpServer.router().post("/multi-part-content-*").handler { ctx ->
            val part1 = ctx.getPart("part1")
            val part2 = ctx.getPart("part2")
            val content = """
                    |received part1: 
                    |${part1.httpFields}
                    |${part1.stringBody}
                    |
                    |received part2:
                    |${part2.stringBody}
                """.trimMargin()
            ctx.end(content)
        }.listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                val part1 = HttpClientContentProviderFactory.stringBody("string content 1")
                val fields1 = HttpFields()
                fields1.put("hello", "hello part 1")
                fields1.addCSV("part123", "1", "2", "3")

                val part2 = HttpClientContentProviderFactory.stringBody("file content 2")
                httpClient.post("$schema://${address.hostName}:${address.port}/multi-part-content-$it")
                    .addPart("part1", part1, fields1)
                    .addFilePart("part2", "fileContent.txt", part2, HttpFields())
                    .submit()
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()
            println(response)
            assertEquals(HttpStatus.OK_200, response.status)
            assertTrue(response.stringBody.contains("hello"))
            assertTrue(response.stringBody.contains("part123"))
            assertTrue(response.stringBody.contains("string content 1"))
            assertTrue(response.stringBody.contains("file content 2"))
        }

        finish(count, time, httpClient, httpServer)
    }

//    @ParameterizedTest
//    @MethodSource("testParametersProvider")
//    @DisplayName("should response 413 when the multi-part too large.")
fun testMultiPartTooLarge(protocol: String, schema: String): Unit = runBlocking {
    val count = 1 // TODO  20 http1 client close connection exception

    val httpConfig = HttpConfig()
    httpConfig.maxUploadFileSize = 10
    httpConfig.maxRequestBodySize = 10
    httpConfig.uploadFileSizeThreshold = 10
    val httpServer = createHttpServer(protocol, schema, httpConfig)
    httpServer.router().post("/multi-part-content-*").handler { ctx ->
        val part1 = ctx.getPart("part1")
            val part2 = ctx.getPart("part2")
            val content = """
                    |received part1: 
                    |${part1.httpFields}
                    |${part1.stringBody}
                    |
                    |received part2:
                    |${part2.stringBody}
                """.trimMargin()
            ctx.end(content)
        }.listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                val part1 = HttpClientContentProviderFactory.stringBody("string content 1")
                val fields1 = HttpFields()
                fields1.put("hello", "hello part 1")
                fields1.addCSV("part123", "1", "2", "3")

                val part2 = HttpClientContentProviderFactory.stringBody("file content 2")

                httpClient.post("$schema://${address.hostName}:${address.port}/multi-part-content-$it")
                    .addPart("part1", part1, fields1)
                    .addFilePart("part2", "fileContent.txt", part2, HttpFields())
                    .submit()
            }
            try {
                withTimeout(Duration.ofSeconds(5).toMillis()) {
                    CompletableFuture.allOf(*futures.toTypedArray()).await()
                    val allDone = futures.all { it.isDone }
                    assertTrue(allDone)

                    val response = futures[0].await()
                    println(response)
                    assertEquals(HttpStatus.PAYLOAD_TOO_LARGE_413, response.status)
                }
            } catch (e: Exception) {
                println(e.message)
            }
        }

        finish(count, time, httpClient, httpServer)
    }

    //    @ParameterizedTest
//    @MethodSource("testParametersProvider")
//    @DisplayName("should response 413 when the http body too large.")
    fun testHttpBodyTooLarge(protocol: String, schema: String): Unit = runBlocking {
        val count = 1 // TODO  20 http1 client close connection exception

        val content = (1..30_000_000).joinToString("") { "a" }
        val httpConfig = HttpConfig()
        httpConfig.maxUploadFileSize = 10
        httpConfig.maxRequestBodySize = 10
        httpConfig.uploadFileSizeThreshold = 10
        val httpServer = createHttpServer(protocol, schema, httpConfig)
        httpServer.router().post("/content-*").handler { ctx ->
            ctx.end("ok")
        }.listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.post("$schema://${address.hostName}:${address.port}/content-$it")
                    .body(content)
                    .submit()
            }
            try {
                withTimeout(Duration.ofSeconds(5).toMillis()) {
                    CompletableFuture.allOf(*futures.toTypedArray()).await()
                    val allDone = futures.all { it.isDone }
                    assertTrue(allDone)

                    val response = futures[0].await()
                    println(response)
                    assertEquals(HttpStatus.PAYLOAD_TOO_LARGE_413, response.status)
                }
            } catch (e: Exception) {
                println(e.message)
            }
        }

        finish(count, time, httpClient, httpServer)
    }

}