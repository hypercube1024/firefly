package com.fireflysource.net.http.server.impl

import com.fireflysource.net.Connection
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.TcpBasedHttpConnection
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.common.v1.decoder.parseAll
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.HttpServerRequest
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher

class Http1ServerConnection(
    private val config: HttpConfig,
    private val tcpConnection: TcpConnection
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, TcpBasedHttpConnection, HttpServerConnection {

    private var connectionListener: HttpServerConnection.Listener? = null

    private val handler = Http1ServerRequestHandler()
    private val parser = HttpParser(handler)
    private val generator = HttpGenerator()

    private suspend fun parseRequest(): HttpServerRequest {
        parser.parseAll(tcpConnection)
        return handler.toHttpServerRequest()
    }

    override fun begin() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listen(listener: HttpServerConnection.Listener): HttpServerConnection {
        connectionListener = listener
        return this
    }

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_1_1

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun getTcpConnection(): TcpConnection = tcpConnection
}