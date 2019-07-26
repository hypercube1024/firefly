package com.fireflysource.net.http.client.impl

import com.fireflysource.net.Connection
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.v2.frame.PingFrame
import com.fireflysource.net.http.common.v2.frame.PriorityFrame
import com.fireflysource.net.http.common.v2.frame.SettingsFrame
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import java.util.concurrent.CompletableFuture

class Http2ClientConnection(
    private val tcpConnection: TcpConnection,
    private val listener: Http2ClientConnectionListener = DefaultHttp2ClientConnectionListener()
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, HttpClientConnection {

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_2

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        TODO("not implemented")
    }

    suspend fun newStream(listener: Http2StreamListener): Http2Stream {
        TODO("not implemented")
    }

    suspend fun sendPriorityFrame(frame: PriorityFrame): Int {
        TODO("not implemented")
    }

    suspend fun sendSettingsFrame(frame: SettingsFrame) {
        TODO("not implemented")
    }

    suspend fun sendPingFrame(frame: PingFrame) {
        TODO("not implemented")
    }

    suspend fun close(error: Int, payload: String) {
        TODO("not implemented")
    }
}