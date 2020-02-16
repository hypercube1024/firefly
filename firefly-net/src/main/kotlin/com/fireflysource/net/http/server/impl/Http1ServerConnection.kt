package com.fireflysource.net.http.server.impl

import com.fireflysource.net.Connection
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.TcpBasedHttpConnection
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.common.v1.decoder.parseAll
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.RoutingContext
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch

class Http1ServerConnection(
    private val config: HttpConfig,
    private val tcpConnection: TcpConnection
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, TcpBasedHttpConnection, HttpServerConnection {

    private val handler = Http1ServerRequestHandler(this)
    private val parser = HttpParser(handler)
    private val generator = HttpGenerator()

    private fun parseRequestAndGenerateResponseJob() = coroutineScope.launch {
        val listener = handler.connectionListener
        requireNotNull(listener)
        while (!tcpConnection.isClosed) {
            try {
                val context = parseRequest()
                listener.onHttpRequestComplete(context).await()
            } catch (e: Exception) {
                listener.onException(handler.getAsyncRoutingContext(), e).await()
            } finally {
                handler.reset()
                parser.reset()
                generator.reset()
            }
        }
    }

    private suspend fun parseRequest(): RoutingContext {
        parser.parseAll(tcpConnection)
        return handler.complete()
    }

    override fun begin() {
        parseRequestAndGenerateResponseJob()
    }

    override fun listen(listener: HttpServerConnection.Listener): HttpServerConnection {
        handler.connectionListener = listener
        return this
    }

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_1_1

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun getTcpConnection(): TcpConnection = tcpConnection
}