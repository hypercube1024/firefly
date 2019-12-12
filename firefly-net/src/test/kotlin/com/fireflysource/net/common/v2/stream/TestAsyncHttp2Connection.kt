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
        val receivedCount = AtomicInteger()
        val channel = Channel<Int>(2)
        val tcpConfig = TcpConfig(30, false)
        val httpConfig = HttpConfig()
        AioTcpServer(tcpConfig).onAcceptAsync { connection ->
            connection.startReadingAndAwaitHandshake()
            val http2Connection = AsyncHttp2Connection(
                2, httpConfig, connection, SimpleFlowControlStrategy(),
                object : Http2Connection.Listener.Adapter() {

                    override fun onPing(http2Connection: Http2Connection, frame: PingFrame) {
                        println("server received ping. ${frame.payloadAsLong}: ${frame.isReply}")
                        channel.offer(receivedCount.incrementAndGet())
                    }
                }
            )
            (1..count).asFlow().map { index ->
                val pingFrame = PingFrame(index.toLong(), false)
                val future = CompletableFuture<Void>()
                http2Connection.ping(pingFrame, futureToConsumer(future))
                future
            }.collect { future -> future.await() }
        }.listen(host, port)

        val client = AioTcpClient(tcpConfig)
        val connection = client.connect(host, port).await()
        connection.startReadingAndAwaitHandshake()
        val http2Connection = AsyncHttp2Connection(
            1, httpConfig, connection, SimpleFlowControlStrategy(),
            object : Http2Connection.Listener.Adapter() {

                override fun onPing(http2Connection: Http2Connection, frame: PingFrame) {
                    println("client received ping. ${frame.payloadAsLong}: ${frame.isReply}")
                    channel.offer(receivedCount.incrementAndGet())
                }
            }
        )
        (1..count).asFlow().map { index ->
            val pingFrame = PingFrame(index.toLong(), false)
            val future = CompletableFuture<Void>()
            http2Connection.ping(pingFrame, futureToConsumer(future))
            future
        }.collect { future -> future.await() }

        while (true) {
            val i = channel.receive()
            if (i == count * 2) {
                break
            }
        }
        @Suppress("BlockingMethodInNonBlockingContext")
        connection.close()
        println(receivedCount.get())
        assertEquals(count * 2, receivedCount.get())

        val stopTime = measureTimeMillis {
            stopAll()
        }
        println("stop success. $stopTime")
    }
}