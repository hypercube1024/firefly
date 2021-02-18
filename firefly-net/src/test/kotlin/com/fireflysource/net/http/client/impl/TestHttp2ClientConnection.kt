package com.fireflysource.net.http.client.impl

import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.client.HttpClient
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpHeaderValue
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.RoutingContext
import com.fireflysource.net.http.server.impl.Http2ServerConnection
import com.fireflysource.net.tcp.TcpServer
import com.fireflysource.net.tcp.TcpServerFactory
import com.fireflysource.net.tcp.onAcceptAsync
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class TestHttp2ClientConnection {

    private lateinit var address: InetSocketAddress

    @BeforeEach
    fun init() {
        address = InetSocketAddress("localhost", Random.nextInt(20000, 40000))
    }

    private fun finish(count: Int, time: Long, httpClient: HttpClient, httpServer: TcpServer) {
        val throughput = count / (time / 1000.00)
        println("success. $time ms, ${throughput.roundToLong()} qps")
        httpClient.stop()
        httpServer.stop()
    }

    private fun createHttpServer(listener: HttpServerConnection.Listener): TcpServer {
        val server = TcpServerFactory.create().timeout(120 * 1000).enableSecureConnection()

        return server.onAcceptAsync { connection ->
            println("accept connection. ${connection.id}")
            connection.beginHandshake().await()
            val http2Connection = Http2ServerConnection(HttpConfig(), connection)
            http2Connection.setListener(listener).begin()
        }.listen(address)
    }

    @Test
    @DisplayName("should send request and receive response successfully.")
    fun testSendRequest(): Unit = runBlocking {
        val count = 10

        val httpServer = createHttpServer(object : HttpServerConnection.Listener.Adapter() {
            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return ctx.put("Test-Http-Exchange", "R1")
                    .end("http exchange success.")
            }
        })

        val httpClient = HttpClientFactory.create()

        val time = measureTimeMillis {
            val futures = (1..count).map { i ->
                httpClient.get("https://${address.hostName}:${address.port}/test/$i").submit()
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()
            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("http exchange success.", response.stringBody)
        }

        finish(count, time, httpClient, httpServer)
    }

    @Test
    @DisplayName("should receive 100 continue response.")
    fun test100Continue(): Unit = runBlocking {
        val count = 100

        val httpServer = createHttpServer(object : HttpServerConnection.Listener.Adapter() {
            override fun onHeaderComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return if (ctx.expect100Continue()) ctx.response100Continue() else Result.DONE
            }

            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return ctx.put("Test-100-Continue", "100")
                    .end("receive data success.")
            }
        })

        val httpClient = HttpClientFactory.create()

        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.post("https://${address.hostName}:${address.port}/data")
                    .put(HttpHeader.EXPECT, HttpHeaderValue.CONTINUE.value)
                    .body("Some test data!")
                    .submit()
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()
            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("receive data success.", response.stringBody)
        }

        finish(count, time, httpClient, httpServer)
    }

    @Test
    @DisplayName("should receive trailers successfully.")
    fun testTrailer(): Unit = runBlocking {
        val count = 100

        val httpServer = createHttpServer(object : HttpServerConnection.Listener.Adapter() {
            override fun onHeaderComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return if (ctx.expect100Continue()) ctx.response100Continue() else Result.DONE
            }

            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return ctx.put("Test-100-Continue", "100")
                    .addCSV(HttpHeader.TRAILER, "t1", "t2", "t3")
                    .setTrailerSupplier {
                        val trailers = HttpFields()
                        trailers.put("t1", "v1")
                        trailers.put("t2", "v2")
                        trailers.put("t3", "v3")
                        trailers
                    }
                    .end("receive data success.")
            }
        })

        val httpClient = HttpClientFactory.create()

        val time = measureTimeMillis {
            val futures = (1..count).map {
                httpClient.post("https://${address.hostName}:${address.port}/data")
                    .put(HttpHeader.EXPECT, HttpHeaderValue.CONTINUE.value)
                    .body("Some test data!")
                    .submit()
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            assertTrue(allDone)

            val response = futures[0].await()
            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals("receive data success.", response.stringBody)
            assertTrue(response.trailerSupplier != null)
            val trailers = response.trailerSupplier.get()
            println(trailers)
            assertEquals("v1", trailers["t1"])
            assertEquals("v2", trailers["t2"])
            assertEquals("v3", trailers["t3"])
        }

        finish(count, time, httpClient, httpServer)
    }


}