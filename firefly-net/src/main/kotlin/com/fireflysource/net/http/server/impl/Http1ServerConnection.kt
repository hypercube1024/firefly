package com.fireflysource.net.http.server.impl

import com.fireflysource.common.`object`.Assert
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.Connection
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.TcpBasedHttpConnection
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.exception.HttpServerConnectionListenerNotSetException
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.common.v1.decoder.parseAll
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.CancellationException
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
    private val beginning = AtomicBoolean(false)
    private val channel: Channel<ParseHttpPacketMessage> = Channel(Channel.UNLIMITED)
    private var parseRequestJob: Job? = null
    private var generateResponseJob: Job? = null

    private fun parseRequestJob() = coroutineScope.launch {
        parseLoop@ while (true) {
            when (channel.receive()) {
                is ParseNextHttpPacket -> parseNextHttpPacket()
                is ExitHttpParser -> {
                    log.info { "Exit the HTTP server parser. id: $id" }
                    break@parseLoop
                }
            }
        }
    }

    private suspend fun parseNextHttpPacket() {
        try {
            parser.parseAll(tcpConnection)
        } catch (e: BadMessageException) {
            requestHandler.badMessage(e)
            channel.trySend(ExitHttpParser)
        } catch (e: IOException) {
            log.info { "The TCP connection IO exception. message: ${e.message ?: e.javaClass.name}, id: $id" }
            channel.trySend(ExitHttpParser)
        } catch (e: CancellationException) {
            log.info { "Cancel HTTP1 parsing. message: ${e.message} id: $id" }
            channel.trySend(ExitHttpParser)
        } catch (e: Exception) {
            log.error(e) { "Parse HTTP1 request exception. id: $id" }
        } finally {
            resetParser()
        }
    }

    fun parseNextRequest() {
        channel.trySend(ParseNextHttpPacket)
    }

    suspend fun endHttpParser() {
        channel.trySend(ExitHttpParser)
        parseRequestJob?.join()
    }

    fun resetParser() {
        parser.reset()
    }

    private fun generateResponseJob() = responseHandler.generateResponseJob()

    fun getHeaderBufferSize() = config.headerBufferSize

    fun sendResponseMessage(message: Http1ResponseMessage) = responseHandler.sendResponseMessage(message)

    suspend fun endResponseHandler() {
        responseHandler.endResponseHandler()
        generateResponseJob?.join()
    }

    override fun begin() {
        if (beginning.compareAndSet(false, true)) {
            if (requestHandler.connectionListener === HttpServerConnection.EMPTY_LISTENER) {
                throw HttpServerConnectionListenerNotSetException("Please set connection listener before begin parsing.")
            }
            parseRequestJob = parseRequestJob()
            generateResponseJob = generateResponseJob()
            parseNextRequest()
        }
    }

    override fun setListener(listener: HttpServerConnection.Listener): HttpServerConnection {
        Assert.state(
            !beginning.get(),
            "The HTTP request parser has started. Please set listener before begin parsing."
        )
        requestHandler.connectionListener = listener
        return this
    }

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_1_1

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun getTcpConnection(): TcpConnection = tcpConnection
}

sealed class ParseHttpPacketMessage
object ParseNextHttpPacket : ParseHttpPacketMessage()
object ExitHttpParser : ParseHttpPacketMessage()