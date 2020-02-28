package com.fireflysource.net.http.client.impl

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.client.HttpClient
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.common.v2.frame.DataFrame
import com.fireflysource.net.http.common.v2.frame.GoAwayFrame
import com.fireflysource.net.http.common.v2.frame.HeadersFrame
import com.fireflysource.net.http.common.v2.stream.Http2Connection
import com.fireflysource.net.http.common.v2.stream.SimpleFlowControlStrategy
import com.fireflysource.net.http.common.v2.stream.Stream
import com.fireflysource.net.http.server.impl.Http2ServerConnection
import com.fireflysource.net.tcp.TcpServer
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
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class TestHttp2ClientConnection {

    private lateinit var address: InetSocketAddress
    private val timeout = 5 * 60L

    @BeforeEach
    fun init() {
        address = InetSocketAddress("localhost", Random.nextInt(20000, 40000))
    }

    @AfterEach
    fun destroy() {
        val time = measureTimeMillis { AbstractLifeCycle.stopAll() }
        println("shutdown time: $time ms")
    }

    private fun finish(count: Int, time: Long, httpClient: HttpClient, httpServer: TcpServer) {
        val throughput = count / (time / 1000.00)
        println("success. $time ms, ${throughput.roundToLong()} qps")
        httpClient.stop()
        httpServer.stop()
    }

    @Test
    @DisplayName("should send request and receive response successfully.")
    fun testSendRequest(): Unit = runBlocking {
        val httpConfig = HttpConfig()
        val count = 100

        val httpServer =
            TcpServerFactory.create().timeout(timeout).enableSecureConnection().onAcceptAsync { connection ->
                println("accept connection. id: ${connection.id}, buffer: ${connection.bufferSize}")
                connection.beginHandshake().await()
                val connectionListener = object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                        println("Server receives go away frame: $frame")
                    }

                    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
                        val fields = HttpFields()
                        fields.put("Test-Http-Exchange", "R1")
                        val response = MetaData.Response(HttpVersion.HTTP_2, HttpStatus.OK_200, fields)
                        val headersFrame = HeadersFrame(stream.id, response, null, false)
                        stream.headers(headersFrame) {}

                        val data = BufferUtils.toBuffer("http exchange success.")
                        val dataFrame = DataFrame(stream.id, data, true)
                        stream.data(dataFrame) {}

                        return Stream.Listener.Adapter()
                    }
                }
                Http2ServerConnection(httpConfig, connection, SimpleFlowControlStrategy(), connectionListener).begin()
            }.listen(address)

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
        val httpConfig = HttpConfig()
        val count = 100

        val httpServer =
            TcpServerFactory.create().timeout(timeout).enableSecureConnection().onAcceptAsync { connection ->
                connection.beginHandshake().await()
                val connectionListener = object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                        println("Server receives go away frame: $frame")
                    }

                    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
                        val headers = LinkedList<HeadersFrame>()
                        var responseClient100Continue = false
                        val trailer = HttpFields()
                        var receivedData = false

                        fun response100Continue(stream: Stream) {
                            if (!responseClient100Continue) {
                                val has100Continue = headers
                                    .map { it.metaData.fields }
                                    .any { HttpHeaderValue.CONTINUE.`is`(it[HttpHeader.EXPECT]) }
                                if (has100Continue) {
                                    val continueResponse =
                                        MetaData.Response(HttpVersion.HTTP_2, HttpStatus.CONTINUE_100, HttpFields())
                                    val continueHeaders = HeadersFrame(stream.id, continueResponse, null, false)
                                    stream.headers(continueHeaders) {}
                                    responseClient100Continue = true
                                }
                            }
                        }

                        fun onMessageComplete(stream: Stream) {
                            val fields = HttpFields()
                            fields.put("Test-100-Continue", "100")
                            val response = MetaData.Response(HttpVersion.HTTP_2, HttpStatus.OK_200, fields)
                            val headersFrame = HeadersFrame(stream.id, response, null, false)
                            stream.headers(headersFrame) {}

                            val data = BufferUtils.toBuffer("receive data success.")
                            val dataFrame = DataFrame(stream.id, data, true)
                            stream.data(dataFrame) {}
                        }

                        headers.add(frame)
                        response100Continue(stream)
                        if (frame.isEndStream) {
                            onMessageComplete(stream)
                        }
                        return object : Stream.Listener.Adapter() {

                            override fun onHeaders(stream: Stream, headersFrame: HeadersFrame) {

                                if (receivedData) {
                                    trailer.addAll(headersFrame.metaData.fields)
                                } else {
                                    headers.add(headersFrame)
                                }
                                response100Continue(stream)
                                if (headersFrame.isEndStream) {
                                    onMessageComplete(stream)
                                }
                            }

                            override fun onData(stream: Stream, dataFrame: DataFrame, result: Consumer<Result<Void>>) {
                                receivedData = true
                                response100Continue(stream)
                                if (dataFrame.isEndStream) {
                                    onMessageComplete(stream)
                                }
                                result.accept(Result.SUCCESS)
                            }
                        }
                    }
                }
                Http2ServerConnection(httpConfig, connection, SimpleFlowControlStrategy(), connectionListener).begin()
            }.listen(address)

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
        val httpConfig = HttpConfig()
        val count = 100

        val httpServer =
            TcpServerFactory.create().timeout(timeout).enableSecureConnection().onAcceptAsync { connection ->
                connection.beginHandshake().await()
                val connectionListener = object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                        println("Server receives go away frame: $frame")
                    }

                    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
                        val headers = LinkedList<HeadersFrame>()
                        var responseClient100Continue = false
                        val trailer = HttpFields()
                        var receivedData = false

                        fun response100Continue(stream: Stream) {
                            if (!responseClient100Continue) {
                                val has100Continue = headers
                                    .map { it.metaData.fields }
                                    .any { HttpHeaderValue.CONTINUE.`is`(it[HttpHeader.EXPECT]) }
                                if (has100Continue) {
                                    val continueResponse =
                                        MetaData.Response(HttpVersion.HTTP_2, HttpStatus.CONTINUE_100, HttpFields())
                                    val continueHeaders = HeadersFrame(stream.id, continueResponse, null, false)
                                    stream.headers(continueHeaders) {}
                                    responseClient100Continue = true
                                }
                            }
                        }

                        fun onMessageComplete(stream: Stream) {
                            val fields = HttpFields()
                            fields.put("Test-100-Continue", "100")
                            fields.addCSV(HttpHeader.TRAILER, "t1", "t2", "t3")
                            val response = MetaData.Response(HttpVersion.HTTP_2, HttpStatus.OK_200, fields)
                            val headersFrame = HeadersFrame(stream.id, response, null, false)
                            stream.headers(headersFrame) {}

                            val data = BufferUtils.toBuffer("receive data success.")
                            val dataFrame = DataFrame(stream.id, data, false)
                            stream.data(dataFrame) {}

                            val trailers = HttpFields()
                            trailers.put("t1", "v1")
                            trailers.put("t2", "v2")
                            trailers.put("t3", "v3")
                            val trailersFrame =
                                HeadersFrame(stream.id, MetaData(HttpVersion.HTTP_2, trailers), null, true)
                            stream.headers(trailersFrame) {}
                        }

                        headers.add(frame)
                        response100Continue(stream)
                        if (frame.isEndStream) {
                            onMessageComplete(stream)
                        }
                        return object : Stream.Listener.Adapter() {

                            override fun onHeaders(stream: Stream, headersFrame: HeadersFrame) {

                                if (receivedData) {
                                    trailer.addAll(headersFrame.metaData.fields)
                                } else {
                                    headers.add(headersFrame)
                                }
                                response100Continue(stream)
                                if (headersFrame.isEndStream) {
                                    onMessageComplete(stream)
                                }
                            }

                            override fun onData(stream: Stream, dataFrame: DataFrame, result: Consumer<Result<Void>>) {
                                receivedData = true
                                response100Continue(stream)
                                if (dataFrame.isEndStream) {
                                    onMessageComplete(stream)
                                }
                                result.accept(Result.SUCCESS)
                            }
                        }
                    }
                }
                Http2ServerConnection(httpConfig, connection, SimpleFlowControlStrategy(), connectionListener).begin()
            }.listen(address)

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