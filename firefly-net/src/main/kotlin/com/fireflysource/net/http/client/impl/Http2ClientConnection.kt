package com.fireflysource.net.http.client.impl

import com.fireflysource.net.Connection
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import java.util.concurrent.CompletableFuture

class Http2ClientConnection(
    private val tcpConnection: TcpConnection
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, HttpClientConnection {

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_2

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}