package com.fireflysource.net.http.client.impl

import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.RoutingContext
import com.fireflysource.net.http.server.impl.Http1ServerConnection
import com.fireflysource.net.tcp.TcpClientConnectionFactory
import com.fireflysource.net.tcp.TcpServer
import com.fireflysource.net.tcp.TcpServerFactory
import com.fireflysource.net.tcp.aio.AioTcpChannelGroup
import com.fireflysource.net.tcp.onAcceptAsync
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.URL
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

class TestAsyncHttpClientConnectionManager {

    private lateinit var address: InetSocketAddress
    private lateinit var httpServer: TcpServer

    @BeforeEach
    fun init() {
        address = InetSocketAddress("localhost", Random.nextInt(2000, 5000))
        val listener = object : HttpServerConnection.Listener.Adapter() {
            override fun onHttpRequestComplete(ctx: RoutingContext): CompletableFuture<Void> {
                return ctx.put(HttpHeader.CONTENT_LENGTH, "7").end("test ok")
            }

            override fun onException(context: RoutingContext, e: Throwable): CompletableFuture<Void> {
                e.printStackTrace()
                return Result.DONE
            }
        }
        httpServer = TcpServerFactory.create().timeout(120 * 1000L).enableOutputBuffer()
            .onAcceptAsync { connection ->
                println("accept connection. ${connection.id}")
                connection.beginHandshake().await()
                val http1Connection = Http1ServerConnection(HttpConfig(), connection)
                http1Connection.setListener(listener).begin()
            }.listen(address)
    }

    @AfterEach
    fun destroy() {
        httpServer.stop()
    }

    @Test
    fun test() = runBlocking {
        val config = HttpConfig()
        val connectionFactory = TcpClientConnectionFactory(
            AioTcpChannelGroup("async-http-client"),
            config.isStopTcpChannelGroup,
            config.timeout,
            config.secureEngineFactory
        )
        val manager = AsyncHttpClientConnectionManager(config, connectionFactory)

        repeat(5) {
            val request = AsyncHttpClientRequest()
            request.method = HttpMethod.GET.value
            @Suppress("BlockingMethodInNonBlockingContext")
            request.uri = HttpURI(URL("http://${address.hostName}:${address.port}/test1").toURI())

            val response = manager.send(request).await()
            println("${response.status} ${response.reason}")
            println(response.httpFields)
            println(response.stringBody)
            println()

            assertEquals(HttpStatus.OK_200, response.status)
            assertEquals(7L, response.contentLength)
            assertEquals("test ok", response.stringBody)
        }

        manager.stop()
    }
}