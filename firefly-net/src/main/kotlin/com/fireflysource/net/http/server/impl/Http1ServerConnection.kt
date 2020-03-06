package com.fireflysource.net.http.server.impl

import com.fireflysource.common.`object`.Assert
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.Connection
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.TcpBasedHttpConnection
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.common.v1.decoder.parseAll
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class Http1ServerConnection(
    val config: HttpConfig,
    private val tcpConnection: TcpConnection
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, TcpBasedHttpConnection, HttpServerConnection {

    companion object {
        private val log = SystemLogger.create(Http1ServerConnection::class.java)
    }

    private val requestHandler = Http1ServerRequestHandler(this)
    private val parser = HttpParser(requestHandler)
    private val responseHandler = Http1ServerResponseHandler(this)
    private val begun = AtomicBoolean(false)

    private fun parseRequestJob() = coroutineScope.launch {
        parseLoop@ while (!tcpConnection.isClosed) {
            try {
                parser.parseAll(tcpConnection)
                if (requestHandler.upgradeHttp2Success()) {
                    responseHandler.sendResponseMessage(UpgradeHttp2Success)
                    log.info { "Server upgrades HTTP2 success. Exit HTTP1 parser. id: $id" }
                    break@parseLoop
                }
            } catch (e: Exception) {
                log.error(e) { "Parse HTTP1 request exception. id: $id" }
            } finally {
                resetParser()
            }
        }
    }

    fun resetParser() {
        parser.reset()
    }

    private fun generateResponseJob() = responseHandler.generateResponseJob()

    fun getHeaderBufferSize() = config.headerBufferSize

    fun sendResponseMessage(message: Http1ResponseMessage) = responseHandler.sendResponseMessage(message)

    override fun begin() {
        if (begun.compareAndSet(false, true)) {
            Assert.state(
                requestHandler.connectionListener !== HttpServerConnection.EMPTY_LISTENER,
                "The HTTP1 server connection listener is empty. Please set listener before begin parsing."
            )
            parseRequestJob()
            generateResponseJob()
        }
    }

    override fun setListener(listener: HttpServerConnection.Listener): HttpServerConnection {
        Assert.state(
            !begun.get(),
            "The HTTP request parser has started. Please set listener before begin parsing."
        )
        requestHandler.connectionListener = listener
        return this
    }

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_1_1

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun getTcpConnection(): TcpConnection = tcpConnection
}