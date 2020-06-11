package com.fireflysource.net

import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.net.http.client.HttpClient
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.server.HttpServer
import com.fireflysource.net.http.server.HttpServerFactory
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpClientFactory
import com.fireflysource.net.tcp.TcpServer
import com.fireflysource.net.tcp.TcpServerFactory
import com.fireflysource.net.tcp.aio.AioTcpChannelGroup
import com.fireflysource.net.tcp.aio.TcpConfig

/**
 * @author Pengtao Qiu
 */
object SharedTcpChannelGroup : AbstractLifeCycle() {

    val group = AioTcpChannelGroup("shared-tcp-group")
    val httpClient: HttpClient by lazy { createHttpClient() }

    init {
        start()
    }

    @JvmOverloads
    fun createTcpServer(config: TcpConfig = TcpConfig()): TcpServer {
        val server = TcpServerFactory.create(config)
        server.tcpChannelGroup(group).stopTcpChannelGroup(false)
        return server
    }

    @JvmOverloads
    fun createTcpClient(config: TcpConfig = TcpConfig()): TcpClient {
        val client = TcpClientFactory.create(config)
        client.tcpChannelGroup(group).stopTcpChannelGroup(false)
        return client
    }

    @JvmOverloads
    fun createHttpServer(httpConfig: HttpConfig = HttpConfig()): HttpServer {
        httpConfig.tcpChannelGroup = group
        httpConfig.isStopTcpChannelGroup = false
        return HttpServerFactory.create(httpConfig)
    }

    @JvmOverloads
    fun createHttpClient(httpConfig: HttpConfig = HttpConfig()): HttpClient {
        httpConfig.tcpChannelGroup = group
        httpConfig.isStopTcpChannelGroup = false
        return HttpClientFactory.create(httpConfig)
    }

    override fun destroy() {
        httpClient.stop()
        group.stop()
    }

    override fun init() {
    }
}