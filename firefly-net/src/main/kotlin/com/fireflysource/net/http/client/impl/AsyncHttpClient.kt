package com.fireflysource.net.http.client.impl

import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.HttpClient
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientRequestBuilder
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.websocket.client.WebSocketClientConnectionBuilder
import com.fireflysource.net.websocket.client.impl.AsyncWebSocketClientConnectionBuilder
import com.fireflysource.net.websocket.client.impl.AsyncWebSocketClientConnectionManager
import java.net.URL
import java.util.concurrent.CompletableFuture

class AsyncHttpClient(private val config: HttpConfig = HttpConfig()) : HttpClient, AbstractLifeCycle() {

    companion object {
        private val log = SystemLogger.create(AsyncHttpClient::class.java)
    }

    private val httpClientConnectionManager = AsyncHttpClientConnectionManager(config)
    private val webSocketClientConnectionManager = AsyncWebSocketClientConnectionManager(
        config,
        httpClientConnectionManager.getTcpClient(),
        httpClientConnectionManager.getSecureTcpClient()
    )

    init {
        start()
    }

    override fun get(url: String): HttpClientRequestBuilder {
        return request(HttpMethod.GET, url)
    }

    override fun post(url: String): HttpClientRequestBuilder {
        return request(HttpMethod.POST, url)
    }

    override fun head(url: String): HttpClientRequestBuilder {
        return request(HttpMethod.HEAD, url)
    }

    override fun put(url: String): HttpClientRequestBuilder {
        return request(HttpMethod.PUT, url)
    }

    override fun delete(url: String): HttpClientRequestBuilder {
        return request(HttpMethod.DELETE, url)
    }

    override fun request(method: HttpMethod, url: String): HttpClientRequestBuilder {
        return request(method.value, url)
    }

    override fun request(method: String, url: String): HttpClientRequestBuilder {
        return request(method, HttpURI(url))
    }

    override fun request(method: String, url: URL): HttpClientRequestBuilder {
        return request(method, HttpURI(url.toURI()))
    }

    override fun request(method: String, httpURI: HttpURI): HttpClientRequestBuilder {
        return AsyncHttpClientRequestBuilder(httpClientConnectionManager, method, httpURI, HttpVersion.HTTP_1_1)
    }

    override fun createHttpClientConnection(httpURI: HttpURI): CompletableFuture<HttpClientConnection> {
        return httpClientConnectionManager.createHttpClientConnection(httpURI)
    }

    override fun createHttpClientConnection(httpURI: String): CompletableFuture<HttpClientConnection> {
        return createHttpClientConnection(HttpURI(httpURI))
    }

    override fun websocket(): WebSocketClientConnectionBuilder {
        return AsyncWebSocketClientConnectionBuilder(webSocketClientConnectionManager)
    }

    override fun websocket(url: String): WebSocketClientConnectionBuilder {
        return AsyncWebSocketClientConnectionBuilder(webSocketClientConnectionManager).url(url)
    }

    override fun init() {
        log.info { "AsyncHttpClient startup. $config" }
    }

    override fun destroy() {
        httpClientConnectionManager.stop()
    }
}