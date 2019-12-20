package com.fireflysource.net.common.v2.stream

import com.fireflysource.common.lifecycle.AbstractLifeCycle.stopAll
import com.fireflysource.net.http.client.impl.Http2ClientConnection
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.common.v2.frame.*
import com.fireflysource.net.http.common.v2.frame.SettingsFrame.DEFAULT_SETTINGS_FRAME
import com.fireflysource.net.http.common.v2.stream.Http2Connection
import com.fireflysource.net.http.common.v2.stream.SimpleFlowControlStrategy
import com.fireflysource.net.http.common.v2.stream.Stream
import com.fireflysource.net.http.server.impl.Http2ServerConnection
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.aio.AioTcpServer
import com.fireflysource.net.tcp.aio.TcpConfig
import com.fireflysource.net.tcp.onAcceptAsync
import com.fireflysource.net.tcp.startReadingAndAwaitHandshake
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.system.measureTimeMillis

class TestAsyncHttp2Connection {

    @Test
    fun testPushPromise() = runBlocking {
        val host = "localhost"
        val port = 4024
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()

        AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.startReadingAndAwaitHandshake()
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
                        println("Server creates the remote stream: $stream . the headers: $frame .")


                        val fields = HttpFields()
                        fields.put("Test-Push-Promise-Stream", "P1")
                        val response = MetaData.Response(HttpVersion.HTTP_2, HttpStatus.OK_200, fields)
                        val pushPromiseFrame = PushPromiseFrame(stream.id, 0, response)
                        stream.push(pushPromiseFrame, {
                            if (it.isSuccess) {
                                println("new push stream success: ${it.value} .")
                            } else {
                                println("new a push stream failed")
                                it.throwable?.printStackTrace()
                            }
                        }, Stream.Listener.Adapter())

                        return Stream.Listener.Adapter()
                    }
                }
            )
        }.listen(host, port)


        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.startReadingAndAwaitHandshake()
        val http2Connection: Http2Connection = Http2ClientConnection(
            httpConfig, connection, SimpleFlowControlStrategy(),
            object : Http2Connection.Listener.Adapter() {

                override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                    println("Client receives go away frame: $frame")
                }

                override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
                    println("Client creates the remote stream: $stream . The headers: $frame .")

                    return object : Stream.Listener.Adapter() {

                        override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                            println("Client received push headers: $frame")
                        }
                    }
                }
            }
        )

        val newPushStreamChannel = Channel<Stream>(UNLIMITED)
        val pushPromiseChannel = Channel<PushPromiseFrame>(UNLIMITED)
        val httpFields = HttpFields()
        httpFields.put("Test-New-Stream", "V1")
        val request = MetaData.Request(
            HttpMethod.GET.value,
            HttpURI(URL("http://localhost:8888/test").toURI()),
            HttpVersion.HTTP_2,
            httpFields
        )
        val headersFrame = HeadersFrame(request, null, true)
        http2Connection.newStream(headersFrame,
            {
                if (it.isSuccess) {
                    println("new stream success: ${it.value} .")
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
                    val success = newPushStreamChannel.offer(stream)
                    println("Client received push stream: $stream . $success , $frame")

                    pushPromiseChannel.offer(frame)
                    return Stream.Listener.Adapter()
                }
            })

        val time = measureTimeMillis {
            val newStream = newPushStreamChannel.receive()
            assertEquals(2, newStream.id)
            assertFalse(newStream.isReset)
            assertFalse(newStream.isClosed)

            val frame = pushPromiseChannel.receive()
            assertEquals(1, frame.streamId)
            assertEquals(2, frame.promisedStreamId)
            assertTrue(frame.metaData.isResponse)
            assertEquals("P1", frame.metaData.fields["Test-Push-Promise-Stream"])
        }
        println("new stream time: $time ms")

        http2Connection.close(ErrorCode.NO_ERROR.code, "exit test") {}
        Unit
    }

    @Test
    fun testNewStream() = runBlocking {
        val host = "localhost"
        val port = 4023
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()

        val headersChannel = Channel<HeadersFrame>(UNLIMITED)

        AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.startReadingAndAwaitHandshake()
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
                        headersChannel.offer(frame)

                        return Stream.Listener.Adapter()
                    }
                }
            )
        }.listen(host, port)


        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.startReadingAndAwaitHandshake()
        val http2Connection: Http2Connection = Http2ClientConnection(
            httpConfig, connection, SimpleFlowControlStrategy(),
            object : Http2Connection.Listener.Adapter() {

                override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                    println("Client receives go away frame: $frame")
                }

                override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
                    println("Client creates the remote stream: $stream . The headers: $frame .")

                    return object : Stream.Listener.Adapter() {

                        override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                            println("Client received push headers: $frame")
                        }
                    }
                }
            }
        )

        val newStreamChannel = Channel<Stream>(UNLIMITED)
        val httpFields = HttpFields()
        httpFields.put("Test-New-Stream", "V1")
        val request = MetaData.Request(
            HttpMethod.GET.value,
            HttpURI(URL("http://localhost:8888/test").toURI()),
            HttpVersion.HTTP_2,
            httpFields
        )
        val headersFrame = HeadersFrame(request, null, true)
        http2Connection.newStream(headersFrame,
            {
                if (it.isSuccess) {
                    val success = newStreamChannel.offer(it.value)
                    println("offer new stream success: $success .")
                } else {
                    println("new a stream failed")
                    it.throwable?.printStackTrace()
                }
            },
            object : Stream.Listener.Adapter() {
                override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                    println("Client received headers: $frame")
                }
            })

        val time = measureTimeMillis {
            val newStream = newStreamChannel.receive()
            assertEquals(1, newStream.id)
            assertFalse(newStream.isReset)
            assertFalse(newStream.isClosed)

            val frame = headersChannel.receive()
            assertEquals(1, frame.streamId)
            assertTrue(frame.metaData.isRequest)
            assertEquals("V1", frame.metaData.fields["Test-New-Stream"])
        }
        println("new stream time: $time ms")

        http2Connection.close(ErrorCode.NO_ERROR.code, "exit test") {}
        Unit
    }

    @Test
    fun testGoAway() = runBlocking {
        val host = "localhost"
        val port = 4022
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()

        val channel = Channel<GoAwayFrame>(UNLIMITED)

        AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.startReadingAndAwaitHandshake()
            Http2ServerConnection(
                httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
                        println("Server receives go away frame: $frame")
                        val success = channel.offer(frame)
                        println("put result go away frame: $success")
                    }
                }
            )
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.startReadingAndAwaitHandshake()
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
        val receivedGoAwayFrame = withTimeout(2000) { channel.receive() }
        assertEquals(ErrorCode.INTERNAL_ERROR.code, receivedGoAwayFrame.error)
    }

    @Test
    fun testSettings() = runBlocking {
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

        AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.startReadingAndAwaitHandshake()
            Http2ServerConnection(
                httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }

                    override fun onSettings(http2Connection: Http2Connection, frame: SettingsFrame) {
                        println("server receives settings: $frame")

                        if (frame.settings == settingsFrame.settings) {
                            val success = channel.offer(frame)
                            println("put result settings frame: $success")
                        }
                    }
                }
            )
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.startReadingAndAwaitHandshake()
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

        val receivedSettings = withTimeout(2000) { channel.receive() }
        assertEquals(settingsFrame.settings, receivedSettings.settings)

        http2Connection.close(ErrorCode.NO_ERROR.code, "exit test") {}
        Unit
    }

    @Test
    fun testPing() = runBlocking {
        val host = "localhost"
        val port = 4020
        val count = 10L
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()

        val channel = Channel<Long>(UNLIMITED)


        AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.startReadingAndAwaitHandshake()
            Http2ServerConnection(
                httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
                        failure.printStackTrace()
                    }
                }
            )
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.startReadingAndAwaitHandshake()
        val http2Connection = Http2ClientConnection(
            httpConfig, connection, SimpleFlowControlStrategy(),
            object : Http2Connection.Listener.Adapter() {

                override fun onPing(http2Connection: Http2Connection, frame: PingFrame) {
                    println("Client receives the ping frame. ${frame.payloadAsLong}: ${frame.isReply}")
                    if (frame.payloadAsLong == count) {
                        val success = channel.offer(frame.payloadAsLong)
                        println("put result ping frame: $success")
                    }
                }
            }
        )

        (1..count).forEach { index ->
            val pingFrame = PingFrame(index, false)
            http2Connection.ping(pingFrame) { println("send ping success. $it") }
        }

        val pingCount = withTimeout(2000) { channel.receive() }
        assertTrue(pingCount > 0)

        http2Connection.close(ErrorCode.NO_ERROR.code, "exit test") {}
        Unit
    }

    @AfterEach
    fun destroy() {
        val time = measureTimeMillis { stopAll() }
        println("shutdown time: $time ms")
    }

}