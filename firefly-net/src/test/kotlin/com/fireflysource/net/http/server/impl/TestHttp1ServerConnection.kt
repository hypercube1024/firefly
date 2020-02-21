package com.fireflysource.net.http.server.impl

import com.fireflysource.common.lifecycle.AbstractLifeCycle.stopAll
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.RoutingContext
import com.fireflysource.net.tcp.TcpServerFactory
import com.fireflysource.net.tcp.onAcceptAsync
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class TestHttp1ServerConnection {

    private lateinit var address: InetSocketAddress

    @BeforeEach
    fun init() {
        address = InetSocketAddress("localhost", Random.nextInt(2000, 5000))
    }

    @AfterEach
    fun destroy() {
        val stopTime = measureTimeMillis {
            stopAll()
        }
        println("stop success. $stopTime")
    }

    @Test
    fun test(): Unit = runBlocking {
        val count = 100

        val server = TcpServerFactory.create()
        server.onAcceptAsync { connection ->
            println("accept connection. ${connection.id}")
            connection.beginHandshake().await()
            val http1ServerConnection = Http1ServerConnection(HttpConfig(), connection)
            http1ServerConnection.setListener(object : HttpServerConnection.Listener.Adapter() {
                override fun onHeaderComplete(ctx: RoutingContext): CompletableFuture<Void> {
                    return Result.DONE
                }

                override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                    return ctx.end("hello http1 server!")
                }

                override fun onException(ctx: RoutingContext?, exception: Exception): CompletableFuture<Void> {
                    exception.printStackTrace()
                    return Result.DONE
                }
            }).begin()
        }.listen(address)

        val httpClient = HttpClientFactory.create()
        val time = measureTimeMillis {
            val futures = (1..count).map {
                val future = httpClient.get("http://${address.hostName}:${address.port}/test-$it").submit()
//                println("client send test-$it")
                future
            }
            CompletableFuture.allOf(*futures.toTypedArray()).await()
            val allDone = futures.all { it.isDone }
            Assertions.assertTrue(allDone)

            val response = futures[0].await()

            Assertions.assertEquals(HttpStatus.OK_200, response.status)
            Assertions.assertEquals("hello http1 server!", response.stringBody)
        }

        val throughput = count / (time / 1000.00)
        println("success. $time ms, ${throughput.roundToLong()} qps")
    }
}