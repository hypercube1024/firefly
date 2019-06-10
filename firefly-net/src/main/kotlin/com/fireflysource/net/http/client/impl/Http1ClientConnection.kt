package com.fireflysource.net.http.client.impl

import com.fireflysource.net.Connection
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import java.util.concurrent.CompletableFuture

class Http1ClientConnection(
    private val tcpConnection: TcpConnection
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, HttpClientConnection {

    private val httpGenerator = HttpGenerator()
    private val httpHandler = Http1ClientResponseHandler()
    private val httpParser = HttpParser(httpHandler)

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_1_1

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}