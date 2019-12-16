package com.fireflysource.net.common.v2.stream

import com.fireflysource.common.lifecycle.AbstractLifeCycle.stopAll
import com.fireflysource.common.sys.Result.discard
import com.fireflysource.common.sys.Result.futureToConsumer
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.v2.frame.PingFrame
import com.fireflysource.net.http.common.v2.frame.SettingsFrame
import com.fireflysource.net.http.common.v2.stream.AsyncHttp2Connection
import com.fireflysource.net.http.common.v2.stream.Http2Connection
import com.fireflysource.net.http.common.v2.stream.SimpleFlowControlStrategy
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.aio.AioTcpServer
import com.fireflysource.net.tcp.aio.TcpConfig
import com.fireflysource.net.tcp.onAcceptAsync
import com.fireflysource.net.tcp.startReadingAndAwaitHandshake
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.measureTimeMillis

class TestAsyncHttp2Connection {

    @Test
    fun testSettings() = runBlocking {
        val host = "localhost"
        val port = 4021
        val semaphore = Semaphore(1, 1)
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()

        val settingsReference = AtomicReference<SettingsFrame>()

        AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.startReadingAndAwaitHandshake()
            AsyncHttp2Connection(
                2, httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onSettings(http2Connection: Http2Connection, frame: SettingsFrame) {
                        settingsReference.set(frame)
                        println("server receives settings: $frame")
                        semaphore.release()
                    }
                }
            )
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.startReadingAndAwaitHandshake()
        val http2Connection = AsyncHttp2Connection(
            2, httpConfig, connection, SimpleFlowControlStrategy(),
            object : Http2Connection.Listener.Adapter() {

                override fun onSettings(http2Connection: Http2Connection, frame: SettingsFrame) {
                    println("client receives settings: $frame")
                }
            }
        )
        val settings = SettingsFrame(
            mutableMapOf(
                SettingsFrame.HEADER_TABLE_SIZE to 8192,
                SettingsFrame.ENABLE_PUSH to 1,
                SettingsFrame.MAX_CONCURRENT_STREAMS to 300,
                SettingsFrame.INITIAL_WINDOW_SIZE to 128 * 1024,
                SettingsFrame.MAX_FRAME_SIZE to 1024 * 1024,
                SettingsFrame.MAX_HEADER_LIST_SIZE to 64
            ), false
        )
        http2Connection.settings(settings, discard())

        semaphore.acquire()

        @Suppress("BlockingMethodInNonBlockingContext")
        connection.close()

        assertNotNull(settingsReference.get())
        assertEquals(settings.settings, settingsReference.get().settings)

        stopTest()
    }

    @Test
    fun testPing() = runBlocking {
        val host = "localhost"
        val port = 4020
        val count = 10
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()

        val maxPing = count * 2
        val semaphore = Semaphore(1, 1)
        val receivedCount = AtomicInteger()


        AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.startReadingAndAwaitHandshake()
            val http2Connection = AsyncHttp2Connection(
                2, httpConfig, connection, SimpleFlowControlStrategy(),
                http2ConnectionListener(semaphore, receivedCount, maxPing)
            )
            sendPingFrames(count, http2Connection)
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.startReadingAndAwaitHandshake()
        val http2Connection = AsyncHttp2Connection(
            1, httpConfig, connection, SimpleFlowControlStrategy(),
            http2ConnectionListener(semaphore, receivedCount, maxPing)
        )
        sendPingFrames(count, http2Connection)

        semaphore.acquire()
        @Suppress("BlockingMethodInNonBlockingContext")
        connection.close()

        println(receivedCount.get())
        assertEquals(maxPing, receivedCount.get())

        stopTest()
    }

    private fun stopTest() {
        val stopTime = measureTimeMillis {
            stopAll()
        }
        println("stop success. $stopTime")
    }

    private fun http2ConnectionListener(
        semaphore: Semaphore,
        receivedCount: AtomicInteger,
        maxPing: Int
    ): Http2Connection.Listener.Adapter {
        return object : Http2Connection.Listener.Adapter() {

            override fun onPing(http2Connection: Http2Connection, frame: PingFrame) {
                println("receives the ping frame. ${frame.payloadAsLong}: ${frame.isReply}")
                if (receivedCount.incrementAndGet() == maxPing) {
                    semaphore.release()
                }
            }
        }
    }

    private suspend fun sendPingFrames(count: Int, http2Connection: AsyncHttp2Connection) {
        (1..count).asFlow().map { index ->
            val pingFrame = PingFrame(index.toLong(), false)
            val future = CompletableFuture<Void>()
            http2Connection.ping(pingFrame, futureToConsumer(future))
            future
        }.collect { future -> future.await() }
    }

}