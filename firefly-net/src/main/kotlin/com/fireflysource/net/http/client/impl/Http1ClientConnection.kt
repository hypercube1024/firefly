package com.fireflysource.net.http.client.impl

import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.net.Connection
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.CompletableFuture

class Http1ClientConnection(
    private val tcpConnection: TcpConnection
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, HttpClientConnection {

    private val httpGenerator = HttpGenerator()
    private val httpHandler = Http1ClientResponseHandler()
    private val httpParser = HttpParser(httpHandler)
    private val outChannel = Channel<RequestMessage>(Channel.UNLIMITED)


    init {
        val acceptRequestJob = launchGlobally(tcpConnection.coroutineDispatcher) {

        }
        tcpConnection.onClose {
            outChannel.close()
            acceptRequestJob.cancel()
        }
    }

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_1_1

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        val future = CompletableFuture<HttpClientResponse>()
        outChannel.offer(RequestMessage(toMetaDataRequest(request), future))
        return future
    }

    private data class RequestMessage(
        val request: MetaData.Request,
        val response: CompletableFuture<HttpClientResponse>
    )
}