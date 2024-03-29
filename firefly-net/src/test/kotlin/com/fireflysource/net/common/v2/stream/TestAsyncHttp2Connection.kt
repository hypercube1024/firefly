package com.fireflysource.net.common.v2.stream

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.futureToConsumer
import com.fireflysource.net.http.client.impl.Http2ClientConnection
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.common.v2.frame.*
import com.fireflysource.net.http.common.v2.frame.SettingsFrame.DEFAULT_SETTINGS_FRAME
import com.fireflysource.net.http.common.v2.stream.AsyncHttp2Stream
import com.fireflysource.net.http.common.v2.stream.Http2Connection
import com.fireflysource.net.http.common.v2.stream.SimpleFlowControlStrategy
import com.fireflysource.net.http.common.v2.stream.Stream
import com.fireflysource.net.http.server.impl.Http2ServerConnection
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.aio.AioTcpServer
import com.fireflysource.net.tcp.aio.TcpConfig
import com.fireflysource.net.tcp.onAcceptAsync
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.URL
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import kotlin.system.measureTimeMillis

class TestAsyncHttp2Connection {

    @Test
    @DisplayName("should send priority frame successfully")
    fun testPriority() = runBlocking {
        val host = "localhost"
        val port = 4026
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()

        val server = AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.beginHandshake().await()
            Http2ServerConnection(
                httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                        println("Server receives go away frame: $frame")
                    }

                    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
                        println("Server creates the remote stream: $stream . the headers: $frame .")

                        val fields = HttpFields()
                        fields.put("Test-New-Stream-Response", "R1")
                        if (frame.priority != null) {
                            fields.put("Stream-Priority", "${frame.priority.weight}")
                            fields.put("Dependency-Stream", "${frame.priority.parentStreamId}")
                        }
                        val response = MetaData.Response(HttpVersion.HTTP_2, HttpStatus.OK_200, fields)
                        val headersFrame = HeadersFrame(stream.id, response, null, false)
                        stream.headers(headersFrame) { println("Server response success.") }

                        return Stream.Listener.Adapter()
                    }
                }
            ).begin()
        }.listen(host, port)


        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.beginHandshake().await()
        val http2Connection: Http2Connection = Http2ClientConnection(
            httpConfig, connection, SimpleFlowControlStrategy(),
            createClientHttp2ConnectionListener()
        )

        val responseHeadersChannel = Channel<HeadersFrame>(UNLIMITED)
        val newStreamChannel = Channel<Stream>(UNLIMITED)
        val headersFrame = createRequestHeadersFrame()
        http2Connection.newStream(headersFrame,
            {
                if (it.isSuccess) {
                    val success = newStreamChannel.trySend(it.value)
                    println("offer new stream success: $success .")
                } else {
                    println("new a stream failed")
                    it.throwable?.printStackTrace()
                }
            },
            object : Stream.Listener.Adapter() {
                override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                    println("Client received headers: $frame")
                    responseHeadersChannel.trySend(frame)
                }
            })

        val stream = newStreamChannel.receive()
        val responseHeadersFrame = responseHeadersChannel.receive()
        assertEquals(1, responseHeadersFrame.streamId)
        assertTrue(responseHeadersFrame.metaData.isResponse)
        assertEquals("R1", responseHeadersFrame.metaData.fields["Test-New-Stream-Response"])
        assertNull(responseHeadersFrame.metaData.fields["Dependency-Stream"])


        val priorityFrame = PriorityFrame(stream.id + 2, stream.id, 10, false)
        val headersFrameWithPriority = createRequestHeadersFrame(priorityFrame)
        http2Connection.newStream(headersFrameWithPriority,
            {
                if (it.isSuccess) {
                    val success = newStreamChannel.trySend(it.value)
                    println("offer new stream success: $success .")
                } else {
                    println("new a stream failed")
                    it.throwable?.printStackTrace()
                }
            },
            object : Stream.Listener.Adapter() {
                override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                    println("Client received headers: $frame")
                    responseHeadersChannel.trySend(frame)
                }
            })

        val future = CompletableFuture<Void>()
        http2Connection.priority(
            PriorityFrame(stream.id + 2, stream.id, 15, false),
            futureToConsumer(future)
        )

        val stream2 = newStreamChannel.receive()
        assertEquals(3, stream2.id)
        assertFalse(stream2.isReset)
        val responseHeadersFrame2 = responseHeadersChannel.receive()
        assertEquals(3, responseHeadersFrame2.streamId)
        assertTrue(responseHeadersFrame2.metaData.isResponse)
        assertEquals("1", responseHeadersFrame2.metaData.fields["Dependency-Stream"])
        assertEquals("10", responseHeadersFrame2.metaData.fields["Stream-Priority"])

        future.await()

        http2Connection.close(ErrorCode.NO_ERROR.code, "exit test") {}

        client.stop()
        server.stop()
    }

    @Test
    @DisplayName("should reset stream successfully after the stream sends reset frame")
    fun testResetFrame() = runBlocking {
        val host = "localhost"
        val port = 4025
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()

        val resetFrameChannel = Channel<ResetFrame>(UNLIMITED)

        val server = AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.beginHandshake().await()
            Http2ServerConnection(
                httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                        println("Server receives go away frame: $frame")
                    }

                    override fun onReset(http2Connection: Http2Connection, frame: ResetFrame) {
                        println("Server receives reset frame for an unknown stream. frame: $frame")
                        resetFrameChannel.trySend(frame)
                    }

                    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
                        println("Server creates the remote stream: $stream . the headers: $frame .")

                        val fields = HttpFields()
                        fields.put("Test-New-Stream-Response", "R1")
                        val response = MetaData.Response(HttpVersion.HTTP_2, HttpStatus.OK_200, fields)
                        val headersFrame = HeadersFrame(stream.id, response, null, false)
                        stream.headers(headersFrame) { println("Server response success.") }

                        return object : Stream.Listener.Adapter() {
                            override fun onReset(stream: Stream, frame: ResetFrame) {
                                println("Server receives the reset frame: $frame .")
                                resetFrameChannel.trySend(frame)
                            }
                        }
                    }
                }
            ).begin()
        }.listen(host, port)


        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.beginHandshake().await()
        val http2Connection: Http2Connection = Http2ClientConnection(
            httpConfig, connection, SimpleFlowControlStrategy(),
            createClientHttp2ConnectionListener()
        )

        val newStreamChannel = Channel<Stream>(UNLIMITED)
        val responseHeadersChannel = Channel<HeadersFrame>(UNLIMITED)
        val headersFrame = createRequestHeadersFrame()
        http2Connection.newStream(headersFrame,
            {
                if (it.isSuccess) {
                    val success = newStreamChannel.trySend(it.value)
                    println("offer new stream success: $success .")
                } else {
                    println("new a stream failed")
                    it.throwable?.printStackTrace()
                }
            },
            object : Stream.Listener.Adapter() {
                override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                    println("Client receives headers: $frame")
                    responseHeadersChannel.trySend(frame)
                }

                override fun onReset(stream: Stream, frame: ResetFrame) {
                    println("Client receives reset frame: $frame")
                }
            })

        val time = measureTimeMillis {
            val newStream = newStreamChannel.receive()
            assertEquals(1, newStream.id)
            assertFalse(newStream.isReset)

            val responseHeadersFrame = responseHeadersChannel.receive()
            assertEquals(1, responseHeadersFrame.streamId)
            assertTrue(responseHeadersFrame.metaData.isResponse)
            assertEquals("R1", responseHeadersFrame.metaData.fields["Test-New-Stream-Response"])

            val resetFrame = ResetFrame(newStream.id, ErrorCode.INTERNAL_ERROR.code)
            newStream.reset(resetFrame) {
                println("reset frame success. $it")
            }
            val serverReceivedResetFrame = resetFrameChannel.receive()
            assertTrue(newStream.isReset)
            assertEquals(1, serverReceivedResetFrame.streamId)
            assertEquals(ErrorCode.INTERNAL_ERROR.code, serverReceivedResetFrame.error)
        }

        println("reset stream time: $time ms")

        http2Connection.close(ErrorCode.NO_ERROR.code, "exit test") {}

        client.stop()
        server.stop()
    }

    @Test
    @DisplayName("should create server push stream successfully")
    fun testPushPromise() = runBlocking {
        val host = "localhost"
        val port = 4024
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()

        val server = AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.beginHandshake().await()
            Http2ServerConnection(
                httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                        println("Server receives go away frame: $frame")
                    }

                    override fun onPreface(http2Connection: Http2Connection): MutableMap<Int, Int> {
                        println("Server receives the preface frame.")
                        return DEFAULT_SETTINGS_FRAME.settings
                    }

                    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
                        println("Server creates the remote stream. stream: ${stream}, headers: $frame")

                        val fields = HttpFields()
                        fields.put("Test-Push-Promise-Stream", "P1")
                        val response = MetaData.Response(HttpVersion.HTTP_2, HttpStatus.OK_200, fields)
                        val pushPromiseFrame = PushPromiseFrame(stream.id, 0, response)
                        stream.push(pushPromiseFrame, {
                            if (it.isSuccess) {
                                println("Server creates new push stream success. stream: ${it.value}")
                            } else {
                                println("new a push stream failed")
                                it.throwable?.printStackTrace()
                            }
                        }, Stream.Listener.Adapter())

                        return Stream.Listener.Adapter()
                    }
                }
            ).begin()
        }.listen(host, port)


        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.beginHandshake().await()
        val http2Connection: Http2Connection = Http2ClientConnection(
            httpConfig, connection, SimpleFlowControlStrategy(),
            createClientHttp2ConnectionListener()
        )

        val newPushStreamChannel = Channel<Stream>(UNLIMITED)
        val pushPromiseChannel = Channel<PushPromiseFrame>(UNLIMITED)
        val headersFrame = createRequestHeadersFrame()
        http2Connection.newStream(headersFrame,
            {
                if (it.isSuccess) {
                    println("Client creates new stream success. stream: ${it.value}")
                } else {
                    println("new a stream failed")
                    it.throwable?.printStackTrace()
                }
            },
            object : Stream.Listener.Adapter() {
                override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                    println("Client received headers: $frame")
                }

                override fun onPush(stream: Stream, frame: PushPromiseFrame): Stream.Listener {
                    val success = newPushStreamChannel.trySend(stream)
                    println("Client received push stream: $stream . $success , $frame")

                    pushPromiseChannel.trySend(frame)
                    return Stream.Listener.Adapter()
                }
            })

        val time = measureTimeMillis {
            val newStream = newPushStreamChannel.receive()
            assertEquals(2, newStream.id)
            assertFalse(newStream.isReset)

            val frame = pushPromiseChannel.receive()
            assertEquals(1, frame.streamId)
            assertEquals(2, frame.promisedStreamId)
            assertTrue(frame.metaData.isResponse)
            assertEquals("P1", frame.metaData.fields["Test-Push-Promise-Stream"])
        }
        println("push promise stream time: $time ms")

        http2Connection.close(ErrorCode.NO_ERROR.code, "exit test") {}

        client.stop()
        server.stop()
    }

    @Test
    @DisplayName("should send data frame successfully after the stream creates.")
    fun testData(): Unit = runTest {
        val host = "localhost"
        val port = 4027
        val tcpConfig = TcpConfig(30, true)
        val httpConfig = HttpConfig()

        val server = AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.beginHandshake().await()
            Http2ServerConnection(
                httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                        println("Server receives go away frame: $frame")
                    }

                    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
                        println("Server creates the remote stream: ${stream}. the headers: ${frame}.")

                        val fields = HttpFields()
                        fields.put("Test-New-Stream-Response", "R1")
                        val response = MetaData.Response(HttpVersion.HTTP_2, HttpStatus.OK_200, fields)
                        val headersFrame = HeadersFrame(stream.id, response, null, false)
                        stream.headers(headersFrame) { println("Server response header success.") }

                        val data = BufferUtils.toBuffer("test data frame.")
                        val dataFrame = DataFrame(stream.id, data, true)
                        stream.data(dataFrame) { println("Server response data success.") }

                        return Stream.Listener.Adapter()
                    }
                }
            ).begin()
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.beginHandshake().await()
        val http2Connection: Http2Connection = Http2ClientConnection(
            httpConfig, connection, SimpleFlowControlStrategy(),
            createClientHttp2ConnectionListener()
        )

        val dataFrameChannel = Channel<DataFrame>(UNLIMITED)
        val headersFrame = createRequestHeadersFrame()
        val future = http2Connection.newStream(headersFrame, object : Stream.Listener.Adapter() {
            override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                println("Client received headers: $frame")
            }

            override fun onData(stream: Stream, frame: DataFrame, result: Consumer<Result<Void>>) {
                println("Client received data frame: $frame")
                dataFrameChannel.trySend(frame)
                result.accept(Result.SUCCESS)
            }
        })
        val time = measureTimeMillis {
            val newStream = future.await()
            val dataFrame = dataFrameChannel.receive()
            assertEquals(1, newStream.id)
            assertFalse(newStream.isReset)
            assertEquals("test data frame.", BufferUtils.toString(dataFrame.data))
        }

        println("receive data time: $time")
        http2Connection.close(ErrorCode.NO_ERROR.code, "exit test") {}

        client.stop()
        server.stop()
    }

    @Test
    @DisplayName("should create a new stream successfully")
    fun testNewStream() = runTest {
        val host = "localhost"
        val port = 4023
        val tcpConfig = TcpConfig(30, true)
        val httpConfig = HttpConfig()

        val requestHeadersChannel = Channel<HeadersFrame>(UNLIMITED)
        val responseHeadersChannel = Channel<HeadersFrame>(UNLIMITED)

        val server = AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.beginHandshake().await()
            Http2ServerConnection(
                httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                        println("Server receives go away frame: $frame")
                    }

                    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
                        println("Server creates the remote stream: $stream . the headers: $frame .")
                        requestHeadersChannel.trySend(frame)

                        val fields = HttpFields()
                        fields.put("Test-New-Stream-Response", "R1")
                        val response = MetaData.Response(HttpVersion.HTTP_2, HttpStatus.OK_200, fields)
                        val headersFrame = HeadersFrame(stream.id, response, null, true)
                        stream.headers(headersFrame) { println("Server response success.") }

                        return Stream.Listener.Adapter()
                    }
                }
            ).begin()
        }.listen(host, port)


        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.beginHandshake().await()
        val http2Connection: Http2Connection = Http2ClientConnection(
            httpConfig, connection, SimpleFlowControlStrategy(),
            createClientHttp2ConnectionListener()
        )

        val headersFrame = createRequestHeadersFrame()
        val future = http2Connection.newStream(headersFrame, object : Stream.Listener.Adapter() {
            override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                println("Client received headers: $frame")
                responseHeadersChannel.trySend(frame)
            }
        })

        val time = measureTimeMillis {
            val newStream = future.await()
            assertEquals(1, newStream.id)
            assertFalse(newStream.isReset)
            if (newStream is AsyncHttp2Stream) {
                assertTrue(newStream.getSendWindow() > 0)
                assertEquals(httpConfig.initialStreamRecvWindow, newStream.getRecvWindow())
            }
            val http2ClientConnection = http2Connection as Http2ClientConnection
            assertTrue(http2ClientConnection.getSendWindow() > 0)
            assertEquals(httpConfig.initialSessionRecvWindow, http2ClientConnection.getRecvWindow())

            val requestHeadersFrame = requestHeadersChannel.receive()
            assertEquals(1, requestHeadersFrame.streamId)
            assertTrue(requestHeadersFrame.metaData.isRequest)
            assertEquals("V1", requestHeadersFrame.metaData.fields["Test-New-Stream"])

            val responseHeadersFrame = responseHeadersChannel.receive()
            assertEquals(1, responseHeadersFrame.streamId)
            assertTrue(responseHeadersFrame.metaData.isResponse)
            assertEquals("R1", responseHeadersFrame.metaData.fields["Test-New-Stream-Response"])
        }
        println("new stream time: $time ms")

        http2Connection.close(ErrorCode.NO_ERROR.code, "exit test") {}

        client.stop()
        server.stop()
    }

    private fun createClientHttp2ConnectionListener(): Http2Connection.Listener.Adapter {
        return object : Http2Connection.Listener.Adapter() {

            override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                failure.printStackTrace()
            }

            override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                println("Client receives go away frame: $frame")
            }
        }
    }

    private fun createRequestHeadersFrame(priorityFrame: PriorityFrame? = null): HeadersFrame {
        val httpFields = HttpFields()
        httpFields.put("Test-New-Stream", "V1")
        @Suppress("BlockingMethodInNonBlockingContext")
        val request = MetaData.Request(
            HttpMethod.GET.value,
            HttpURI(URL("http://localhost:8888/test").toURI()),
            HttpVersion.HTTP_2,
            httpFields
        )
        return HeadersFrame(request, priorityFrame, true)
    }

    @Test
    @DisplayName("should send go away frame successfully")
    fun testGoAway() = runTest {
        val host = "localhost"
        val port = 4022
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()

        val server = AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.beginHandshake().await()
            Http2ServerConnection(
                httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                        println("Server receives go away frame: $frame")
                    }
                }
            ).begin()
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.beginHandshake().await()
        val http2Connection = Http2ClientConnection(
            httpConfig, connection, SimpleFlowControlStrategy(),
            object : Http2Connection.Listener.Adapter() {

                override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                    println("Client receives go away frame: $frame")
                }
            }
        )

        val success = http2Connection.close(ErrorCode.INTERNAL_ERROR.code, "test error message")
        assertTrue(success)

        client.stop()
        server.stop()
    }

    @Test
    @DisplayName("should send settings frame successfully")
    fun testSettings() = runTest {
        val host = "localhost"
        val port = 4021
        val channel = Channel<SettingsFrame>(UNLIMITED)
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()

        val settingsFrame = SettingsFrame(
            mutableMapOf(
                SettingsFrame.HEADER_TABLE_SIZE to 8192,
                SettingsFrame.ENABLE_PUSH to 1,
                SettingsFrame.MAX_CONCURRENT_STREAMS to 300,
                SettingsFrame.INITIAL_WINDOW_SIZE to 128 * 1024,
                SettingsFrame.MAX_FRAME_SIZE to 1024 * 1024,
                SettingsFrame.MAX_HEADER_LIST_SIZE to 64
            ), false
        )

        val server = AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.beginHandshake().await()
            Http2ServerConnection(
                httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onSettings(http2Connection: Http2Connection, frame: SettingsFrame) {
                        println("server receives settings: $frame")

                        if (frame.settings == settingsFrame.settings) {
                            val success = channel.trySend(frame)
                            println("put result settings frame: $success")
                        }
                    }
                }
            ).begin()
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.beginHandshake().await()
        val http2Connection = Http2ClientConnection(
            httpConfig, connection, SimpleFlowControlStrategy(),
            object : Http2Connection.Listener.Adapter() {

                override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                    failure.printStackTrace()
                }

                override fun onSettings(http2Connection: Http2Connection, frame: SettingsFrame) {
                    println("client receives settings: $frame")
                }
            }
        )

        http2Connection.settings(settingsFrame) { println("send settings success. $it") }

        val receivedSettings = channel.receive()//withTimeout(2000) { channel.receive() }
        assertEquals(settingsFrame.settings, receivedSettings.settings)

        http2Connection.close(ErrorCode.NO_ERROR.code, "exit test") {}

        client.stop()
        server.stop()
    }

    @Test
    @DisplayName("should send ping frame successfully")
    fun testPing() = runTest {
        val host = "localhost"
        val port = 4020
        val count = 10L
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()

        val channel = Channel<Long>(UNLIMITED)


        val server = AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.beginHandshake().await()
            Http2ServerConnection(
                httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }
                }
            ).begin()
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.beginHandshake().await()
        val http2Connection = Http2ClientConnection(
            httpConfig, connection, SimpleFlowControlStrategy(),
            object : Http2Connection.Listener.Adapter() {

                override fun onPing(http2Connection: Http2Connection, frame: PingFrame) {
                    println("Client receives the ping frame. ${frame.payloadAsLong}: ${frame.isReply}")
                    if (frame.payloadAsLong == count) {
                        val success = channel.trySend(frame.payloadAsLong)
                        println("put result ping frame: $success")
                    }
                }
            }
        )

        (1..count).forEach { index ->
            val pingFrame = PingFrame(index, false)
            http2Connection.ping(pingFrame) { println("send ping success. $it") }
        }

        val pingCount = channel.receive()//withTimeout(20000) { channel.receive() }
        assertTrue(pingCount > 0)

        http2Connection.close(ErrorCode.NO_ERROR.code, "exit test") {}

        client.stop()
        server.stop()
    }

    @Test
    @DisplayName("should receive reset frame when the stream idle timeout")
    fun testStreamIdleTimeout(): Unit = runTest {
        val host = "localhost"
        val port = 4100
        val tcpConfig = TcpConfig(30, false)
        val serverHttpConfig = HttpConfig()
        serverHttpConfig.streamIdleTimeout = 1

        val server = AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.beginHandshake().await()
            Http2ServerConnection(
                serverHttpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                        println("Server receives go away frame: $frame")
                    }

                    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
                        println("Server creates the remote stream: $stream . the headers: $frame .")

                        val fields = HttpFields()
                        fields.put("Test-Idle-Timeout", "R1")
                        val response = MetaData.Response(HttpVersion.HTTP_2, HttpStatus.OK_200, fields)
                        val headersFrame = HeadersFrame(stream.id, response, null, false)
                        stream.headers(headersFrame) { println("Server response success.") }
                        return object : Stream.Listener.Adapter() {
                            override fun onReset(stream: Stream, frame: ResetFrame) {
                                println("Server received reset: $frame")
                            }

                            override fun onIdleTimeout(stream: Stream, x: Throwable): Boolean {
                                println("${x.message}: $stream")
                                return true
                            }
                        }
                    }
                }
            ).begin()
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.beginHandshake().await()
        val http2Connection: Http2Connection = Http2ClientConnection(
            HttpConfig(), connection, SimpleFlowControlStrategy(),
            createClientHttp2ConnectionListener()
        )

        val headersFrame = createRequestHeadersFrame()
        val channel: Channel<ResetFrame> = Channel(UNLIMITED)
        val future = http2Connection.newStream(headersFrame, object : Stream.Listener.Adapter() {
            override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                println("Client received headers: $frame")
            }

            override fun onReset(stream: Stream, frame: ResetFrame) {
                println("Client received reset: $frame")
                channel.trySend(frame)
            }
        })

        val time = measureTimeMillis {
            val newStream = future.await()
            assertEquals(1, newStream.id)
            val resetFrame = channel.receive()
            assertEquals(1, resetFrame.streamId)
        }
        println("new stream time: $time ms")

        http2Connection.close(ErrorCode.NO_ERROR.code, "exit test") {}

        client.stop()
        server.stop()
    }

    @Test
    @DisplayName("should set the stream idle timeout successfully")
    fun testSetStreamIdleTimeout(): Unit = runTest {
        val host = "localhost"
        val port = 4101
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()
        httpConfig.streamIdleTimeout = 30

        val channel: Channel<ResetFrame> = Channel(UNLIMITED)
        val server = AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.beginHandshake().await()
            Http2ServerConnection(
                httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                        println("Server receives go away frame: $frame")
                    }

                    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
                        println("Server creates the remote stream: $stream . the headers: $frame .")

                        val fields = HttpFields()
                        fields.put("Test-Idle-Timeout", "R1")
                        val response = MetaData.Response(HttpVersion.HTTP_2, HttpStatus.OK_200, fields)
                        val headersFrame = HeadersFrame(stream.id, response, null, false)
                        stream.headers(headersFrame) { println("Server response success.") }
                        return object : Stream.Listener.Adapter() {
                            override fun onReset(stream: Stream, frame: ResetFrame) {
                                println("Server received reset: $frame")
                                channel.trySend(frame)
                            }

                            override fun onIdleTimeout(stream: Stream, x: Throwable): Boolean {
                                println("${x.message}: $stream")
                                return true
                            }
                        }
                    }
                }
            ).begin()
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.beginHandshake().await()
        val http2Connection: Http2Connection = Http2ClientConnection(
            httpConfig, connection, SimpleFlowControlStrategy(),
            createClientHttp2ConnectionListener()
        )

        val headersFrame = createRequestHeadersFrame()

        val future = http2Connection.newStream(headersFrame, object : Stream.Listener.Adapter() {
            override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                println("Client received headers: $frame")
            }

            override fun onReset(stream: Stream, frame: ResetFrame) {
                println("Client received reset: $frame")
            }
        })

        val time = measureTimeMillis {
            val newStream = future.await()
            assertEquals(1, newStream.id)
            newStream.idleTimeout = 1
            val resetFrame = channel.receive()
            assertEquals(1, resetFrame.streamId)
        }
        println("new stream time: $time ms")

        http2Connection.close(ErrorCode.NO_ERROR.code, "exit test") {}

        client.stop()
        server.stop()
    }

}