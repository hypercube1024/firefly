package com.fireflysource.net.common.v2.stream

import com.fireflysource.common.lifecycle.AbstractLifeCycle.stopAll
import com.fireflysource.common.sys.Result.futureToConsumer
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.v2.frame.PingFrame
import com.fireflysource.net.http.common.v2.stream.AsyncHttp2Connection
import com.fireflysource.net.http.common.v2.stream.Http2Connection
import com.fireflysource.net.http.common.v2.stream.SimpleFlowControlStrategy
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.aio.AioTcpServer
import com.fireflysource.net.tcp.aio.TcpConfig
import com.fireflysource.net.tcp.onAcceptAsync
import com.fireflysource.net.tcp.startReadingAndAwaitHandshake
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class TestAsyncHttp2Connection {

    @Test
    fun testPing() = runBlocking {
        val host = "localhost"
        val port = 4020
        val count = 10
        val maxPing = count * 2
        val receivedCount = AtomicInteger()
        val channel = Channel<Int>()
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()


        AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.startReadingAndAwaitHandshake()
            val http2Connection = AsyncHttp2Connection(
                2, httpConfig, connection, SimpleFlowControlStrategy(),
                http2ConnectionListener(channel, receivedCount, maxPing)
            )
            sendPingFrames(count, http2Connection)
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.startReadingAndAwaitHandshake()
        val http2Connection = AsyncHttp2Connection(
            1, httpConfig, connection, SimpleFlowControlStrategy(),
            http2ConnectionListener(channel, receivedCount, maxPing)
        )
        sendPingFrames(count, http2Connection)

        wait(channel, maxPing)
        @Suppress("BlockingMethodInNonBlockingContext")
        connection.close()
        println(receivedCount.get())
        assertEquals(maxPing, receivedCount.get())

        val stopTime = measureTimeMillis {
            stopAll()
        }
        println("stop success. $stopTime")
    }

    private fun http2ConnectionListener(channel: Channel<Int>, receivedCount: AtomicInteger, maxPing: Int): Http2Connection.Listener.Adapter {
        return object : Http2Connection.Listener.Adapter() {

            override fun onPing(http2Connection: Http2Connection, frame: PingFrame) {
                println("receives the ping frame. ${frame.payloadAsLong}: ${frame.isReply}")
                if (receivedCount.incrementAndGet() == maxPing) {
                    channel.offer(maxPing)
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

    private suspend fun wait(channel: Channel<Int>, maxPing: Int) {
        while (true) {
            val i = channel.receive()

            if (i == maxPing) {
                break
            }
        }
    }
}