package com.fireflysource.net.http.server.impl

import com.fireflysource.common.`object`.Assert
import com.fireflysource.net.Connection
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.TcpBasedHttpConnection
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.common.v1.decoder.parseAll
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.HttpServerOutputChannel
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class Http1ServerConnection(
    private val config: HttpConfig,
    private val tcpConnection: TcpConnection
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, TcpBasedHttpConnection, HttpServerConnection {

    private val handler = Http1ServerRequestHandler(this)
    private val parser = HttpParser(handler)
    private val startParsing = AtomicBoolean(false)

    private fun parseRequestJob() = coroutineScope.launch {
        val listener = handler.connectionListener
        requireNotNull(listener)
        while (!tcpConnection.isClosed) {
            try {
                parseRequest()
            } finally {
                parser.reset()
            }
        }
    }

    private suspend fun parseRequest() {
        parser.parseAll(tcpConnection)
        return handler.waitMessageComplete()
    }

    override fun begin() {
        if (startParsing.compareAndSet(false, true)) {
            parseRequestJob()
        }
    }

    override fun setListener(listener: HttpServerConnection.Listener): HttpServerConnection {
        Assert.state(
            !startParsing.get(),
            "The HTTP request parser has started. Please set listener before begin parsing."
        )

        handler.connectionListener = listener
        return this
    }

    override fun createHttpServerOutputChannel(): HttpServerOutputChannel {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_1_1

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun getTcpConnection(): TcpConnection = tcpConnection
}