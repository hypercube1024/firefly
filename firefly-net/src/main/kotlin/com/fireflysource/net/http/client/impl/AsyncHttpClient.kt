package com.fireflysource.net.http.client.impl

import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.HttpClient
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientRequestBuilder
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.tcp.TcpClientConnectionFactory
import com.fireflysource.net.tcp.aio.AioTcpChannelGroup
import com.fireflysource.net.websocket.client.WebSocketClientConnectionBuilder
import com.fireflysource.net.websocket.client.impl.AsyncWebSocketClientConnectionBuilder
import com.fireflysource.net.websocket.client.impl.AsyncWebSocketClientConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

class AsyncHttpClient(private val config: HttpConfig = HttpConfig()) : HttpClient, AbstractLifeCycle() {

    companion object {
        private val log = SystemLogger.create(AsyncHttpClient::class.java)
    }

    private val connectionFactory = TcpClientConnectionFactory(
        createTcpChannelGroup(),
        config.isStopTcpChannelGroup,
        config.timeout,
        config.secureEngineFactory
    )
    private val httpClientConnectionManager = AsyncHttpClientConnectionManager(config, connectionFactory)
    private val webSocketClientConnectionManager = AsyncWebSocketClientConnectionManager(config, connectionFactory)

    init {
        start()
    }

    private fun createTcpChannelGroup() =
        if (config.tcpChannelGroup != null) config.tcpChannelGroup
        else AioTcpChannelGroup("async-http-client")

    override fun request(method: String, httpURI: HttpURI): HttpClientRequestBuilder {
        return AsyncHttpClientRequestBuilder(httpClientConnectionManager, method, httpURI, HttpVersion.HTTP_1_1)
    }

    override fun createHttpClientConnection(
        httpURI: HttpURI,
        supportedProtocols: List<String>
    ): CompletableFuture<HttpClientConnection> {
        return httpClientConnectionManager.createHttpClientConnection(httpURI, supportedProtocols)
    }

    override fun websocket(): WebSocketClientConnectionBuilder {
        return AsyncWebSocketClientConnectionBuilder(webSocketClientConnectionManager)
    }

    override fun websocket(url: String): WebSocketClientConnectionBuilder {
        return AsyncWebSocketClientConnectionBuilder(webSocketClientConnectionManager).url(url)
    }

    override fun init() {
        log.info { "AsyncHttpClient startup. Config: $config" }
    }

    override fun destroy() {
        httpClientConnectionManager.stop()
        webSocketClientConnectionManager.stop()
    }
}

fun HttpClient.connectAsync(uri: String, block: suspend CoroutineScope.(HttpClientConnection) -> Unit) {
    this.createHttpClientConnection(uri)
        .thenAccept { connection -> connection.coroutineScope.launch { block(connection) } }
}

fun HttpClient.connectAsync(
    httpURI: HttpURI,
    supportedProtocols: List<String>,
    block: suspend CoroutineScope.(HttpClientConnection) -> Unit
) {
    this.createHttpClientConnection(httpURI, supportedProtocols)
        .thenAccept { connection -> connection.coroutineScope.launch { block(connection) } }
}

fun HttpClient.connectAsync(
    httpURI: HttpURI,
    block: suspend CoroutineScope.(HttpClientConnection) -> Unit
) {
    this.createHttpClientConnection(httpURI)
        .thenAccept { connection -> connection.coroutineScope.launch { block(connection) } }
}