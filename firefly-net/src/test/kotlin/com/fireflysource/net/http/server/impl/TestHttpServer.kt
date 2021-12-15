package com.fireflysource.net.http.server.impl

import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.ProxyConfig
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.HttpServerContentProviderFactory
import com.fireflysource.net.http.server.HttpServerFactory
import com.fireflysource.net.http.server.impl.router.asyncBlockingHandler
import com.fireflysource.net.http.server.impl.router.asyncHandler
import com.fireflysource.net.http.server.impl.router.getCurrentRoutingContext
import com.fireflysource.net.tcp.TcpClientFactory
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@Suppress("HttpUrlsUsage")
class TestHttpServer : AbstractHttpServerTestBase() {

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should visit router chain successfully.")
    fun testRouterChain(protocol: String, schema: String) = runTest {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        httpServer
            .router().path("*").asyncHandler { ctx ->
                assertTrue(getCurrentRoutingContext() != null)
                ctx.write("into router -> ")
                ctx.next().await()
                ctx.end("end router.")
            }
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
                "into router -> visit foo: bar |visit pattern: bar |regex group: bar |end router.",
                response.stringBody
            )
            println(response)
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should enable and disable router successfully.")
    fun testEnableAndDisableRouter(protocol: String, schema: String): Unit = runTest {
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
    fun testContextAttributes(protocol: String, schema: String): Unit = runTest {
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
    fun testRouterNotCommit(protocol: String, schema: String): Unit = runTest {
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

            try {
                val response = futures[0].await()

                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.status)
                println(response)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        finish(count, time, httpClient, httpServer)
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should response 400 when the resource not found.")
    fun testResourceNotFound(protocol: String, schema: String): Unit = runTest {
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

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should response 500 when the router happens exception.")
    fun testRouterException(protocol: String, schema: String): Unit = runTest {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        httpServer
            .router().get("/exception").handler {
                throw IllegalStateException("test exception")
            }
            .router().get("/exception").handler { ctx ->
                println("bad end?")
                ctx.end("bad end")
            }
            .listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count)
                .map { httpClient.get("$schema://${address.hostName}:${address.port}/exception").submit() }
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

    @Test
    @DisplayName("should response 400 when the host header is missing.")
    fun testNoHostHeader(): Unit = runTest {
        val httpServer = createHttpServer("http1", "http")
        httpServer
            .router().get("/testNoHost").handler { it.end("ok") }
            .listen(address)

        val config = HttpConfig()
        config.isAutoGeneratedClientHttp1Headers = false
        val httpClient = HttpClientFactory.create(config)

        val time = measureTimeMillis {
            val response = httpClient.get("http://${address.hostName}:${address.port}/testNoHost").submit().await()
            assertEquals(HttpStatus.BAD_REQUEST_400, response.status)
            println(response)
        }

        finish(1, time, httpClient, httpServer)
    }

    @Test
    @DisplayName("should establish HTTP tunnel successfully.")
    fun testHttpTunnel(): Unit = runTest {
        val httpServer = createHttpServer("http1", "http")
        httpServer
            .onAcceptHttpTunnel { request ->
                println("Accept http tunnel request. $request")
                CompletableFuture.completedFuture(true)
            }
            .onHttpTunnelHandshakeComplete { connection, address ->
                println("target address: $address")
                connection.write(BufferUtils.toBuffer("1234"))
                    .thenCompose { connection.closeAsync() }
            }
            .listen(address)

        val message = "CONNECT p54-caldav.icloud.com:443 HTTP/1.1\r\n" +
                "Host: p54-caldav.icloud.com\r\n" +
                "User-Agent: Mac+OS+X/10.15.7 (19H114) CalendarAgent/930.5.1\r\n" +
                "Connection: keep-alive\r\n" +
                "Proxy-Connection: keep-alive\r\n\r\n"
        val tcpClient = TcpClientFactory.create()
        val connection = tcpClient.connect(address).await()
        connection.write(BufferUtils.toBuffer(message)).await()
        val receivedData = mutableListOf<ByteBuffer>()
        while (true) {
            try {
                val data = connection.read().await()
                receivedData.add(data)
            } catch (e: ClosedChannelException) {
                break
            }
        }
        val receivedMessages = BufferUtils.toString(receivedData, StandardCharsets.UTF_8)
        println(receivedMessages)
        assertTrue(receivedMessages.isNotBlank())
        assertTrue(receivedMessages.contains("HTTP/1.1 200 Connection Established\r\n\r\n"))
        assertTrue(receivedMessages.contains("1234"))

        tcpClient.stop()
        httpServer.stop()
    }

    @Test
    @DisplayName("should establish HTTP tunnel failure.")
    fun testHttpTunnelFailure(): Unit = runTest {
        val httpServer = createHttpServer("http1", "http")
        httpServer
            .onAcceptHttpTunnel { request ->
                println("Accept http tunnel request. $request")
                CompletableFuture.completedFuture(false)
            }
            .onHttpTunnelHandshakeComplete { connection, address ->
                println("target address: $address")
                connection.write(BufferUtils.toBuffer("1234"))
                    .thenCompose { connection.closeAsync() }
            }
            .listen(address)

        val message = "CONNECT p54-caldav.icloud.com:443 HTTP/1.1\r\n" +
                "Host: p54-caldav.icloud.com\r\n" +
                "User-Agent: Mac+OS+X/10.15.7 (19H114) CalendarAgent/930.5.1\r\n" +
                "Connection: keep-alive\r\n" +
                "Proxy-Connection: keep-alive\r\n\r\n"
        val tcpClient = TcpClientFactory.create()
        val connection = tcpClient.connect(address).await()
        connection.write(BufferUtils.toBuffer(message)).await()
        val receivedData = mutableListOf<ByteBuffer>()
        while (true) {
            try {
                val data = connection.read().await()
                receivedData.add(data)
            } catch (e: ClosedChannelException) {
                break
            }
        }
        val receivedMessages = BufferUtils.toString(receivedData, StandardCharsets.UTF_8)
        println(receivedMessages)
        assertTrue(receivedMessages.isNotBlank())
        assertTrue(receivedMessages.contains("Proxy Authentication Required"))
        assertTrue(receivedMessages.contains("The proxy authentication must be required"))
        assertFalse(receivedMessages.contains("1234"))

        tcpClient.stop()
        httpServer.stop()
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should send request via the http proxy successfully.")
    fun testHttpProxy(protocol: String, schema: String): Unit = runTest {
        val count = 1

        val httpServer = createHttpServer(protocol, schema)
        httpServer
            .router().path("/hello/*").asyncHandler { ctx ->
                assertTrue(getCurrentRoutingContext() != null)
                ctx.write("into router -> ")
                ctx.next().await()
                ctx.end("end router.")
            }
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
            .router().get("/length/test").handler { ctx ->
                ctx.contentProvider(HttpServerContentProviderFactory.stringBody("1234567", StandardCharsets.UTF_8))
                    .end()
            }
            .listen(address)

        val proxyAddress = InetSocketAddress("localhost", Random.nextInt(10000, 20000))
        val proxy = HttpServerFactory.createHttpProxy()
        proxy.listen(proxyAddress)

        val clientHttpConfig = HttpConfig()
        val proxyConfig = ProxyConfig()
        proxyConfig.host = proxyAddress.hostName
        proxyConfig.port = proxyAddress.port
        clientHttpConfig.proxyConfig = proxyConfig
        val httpClient = HttpClientFactory.create(clientHttpConfig)
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
                "into router -> visit foo: bar |visit pattern: bar |regex group: bar |end router.",
                response.stringBody
            )
            println(response)

            val lengthResponse =
                httpClient.get("$schema://${address.hostName}:${address.port}/length/test").submit().await()
            assertEquals(HttpStatus.OK_200, lengthResponse.status)
            assertEquals("1234567", lengthResponse.stringBody)
            println(lengthResponse)
        }

        finish(count, time, httpClient, httpServer)
        proxy.stop()
    }

    @Test
    fun testCopy() = runTest {
        val httpServer = createHttpServer("http1", "http")
        httpServer.router().get("/testCopy")
            .asyncBlockingHandler {
                println(Thread.currentThread().name)
                it.end("Origin server")
            }
            .router().get("/blockingTask")
            .blockingHandler {
                Thread.sleep(100)
                println(Thread.currentThread().name)
                it.end("Blocking task").get()
            }
            .listen(address)

        var newAddress = InetSocketAddress("localhost", Random.nextInt(20000, 40000))
        while (newAddress == address) {
            newAddress = InetSocketAddress("localhost", Random.nextInt(20000, 40000))
        }
        val newHttpServer = httpServer.copy()
        newHttpServer.enableSecureConnection().listen(newAddress)

        val httpClient = HttpClientFactory.create()

        var response = httpClient.get("http://${address.hostName}:${address.port}/testCopy").submit().await()
        assertEquals("Origin server", response.stringBody)
        println(response)

        response = httpClient.get("http://${address.hostName}:${address.port}/blockingTask").submit().await()
        assertEquals("Blocking task", response.stringBody)
        println(response)

        response = httpClient.get("https://${newAddress.hostName}:${newAddress.port}/testCopy").submit().await()
        assertEquals("Origin server", response.stringBody)
        println(response)

        response = httpClient.get("https://${newAddress.hostName}:${newAddress.port}/blockingTask").submit().await()
        assertEquals("Blocking task", response.stringBody)
        println(response)

        httpClient.stop()
        httpServer.stop()
        newHttpServer.stop()
    }
}