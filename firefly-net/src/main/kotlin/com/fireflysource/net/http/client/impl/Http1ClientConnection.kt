package com.fireflysource.net.http.client.impl

import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.io.useAwait
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.Connection
import com.fireflysource.net.http.client.*
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.expectUpgradeHttp2
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.TcpBasedHttpConnection
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.common.v1.decoder.parse
import com.fireflysource.net.http.common.v1.decoder.parseAll
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator.Result.*
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator.State.*
import com.fireflysource.net.http.common.v1.encoder.assert
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.function.Predicate

class Http1ClientConnection(
    private val config: HttpConfig,
    private val tcpConnection: TcpConnection
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, TcpBasedHttpConnection, HttpClientConnection {

    companion object {
        private val log = SystemLogger.create(Http1ClientConnection::class.java)
    }

    private val generator = HttpGenerator()

    private val headerBuffer: ByteBuffer by lazy { BufferUtils.allocateDirect(config.headerBufferSize) }
    private val contentBuffer: ByteBuffer by lazy { BufferUtils.allocateDirect(config.contentBufferSize) }
    private val chunkBuffer: ByteBuffer by lazy { BufferUtils.allocateDirect(HttpGenerator.CHUNK_SIZE) }

    private val handler = Http1ClientResponseHandler()
    private val parser = HttpParser(handler)

    private val requestChannel = Channel<RequestMessage>(Channel.UNLIMITED)
    private var httpVersion: HttpVersion = HttpVersion.HTTP_1_1
    private var http2ClientConnection: Http2ClientConnection? = null

    init {
        generateRequestAndParseResponseJob()
    }

    private fun generateRequestAndParseResponseJob() = coroutineScope.launch {
        jobLoop@ while (true) {
            val message = requestChannel.receive()
            try {
                if (message.expectUpgradeHttp2) {
                    log.info { "Prepare to upgrade HTTP2. id: $id" }
                }
                handler.init(message.contentHandler, message.expectServerAcceptsContent, message.expectUpgradeHttp2)
                generateRequestAndFlushData(message)
                log.debug("HTTP1 client generates request complete. id: $id")
                parseResponse(message).complete(message)
                if (message.expectUpgradeHttp2 && isUpgradeToHttp2Success()) break@jobLoop
            } catch (e: Exception) {
                log.error(e) { "HTTP1 client handler exception. id: $id" }
                message.response.completeExceptionally(e)
            } finally {
                handler.reset()
                parser.reset()
                generator.reset()
            }
        }
        if (isUpgradeToHttp2Success()) {
            while (true) {
                val message = requestChannel.poll()
                if (message != null) {
                    val future = message.response
                    sendRequestViaHttp2(message.httpClientRequest)
                        .thenAccept { future.complete(it) }
                        .exceptionallyAccept { future.completeExceptionally(it) }
                } else break
            }
        }
    }

    private suspend fun HttpClientResponse.complete(message: RequestMessage) {
        val request = message.request
        val response = this
        if (response.httpFields.isCloseConnection(response.httpVersion) ||
            request.fields.isCloseConnection(request.httpVersion)
        ) {
            this@Http1ClientConnection.useAwait { message.response.complete(response) }
        } else message.response.complete(response)
    }

    private suspend fun parseResponse(message: RequestMessage): HttpClientResponse {
        if (message.expectUpgradeHttp2) {
            val buffer = parser.parse(tcpConnection, Predicate { it.ordinal >= HttpParser.State.HEADER.ordinal })
            val success = handler.upgradeHttp2Successfully()
            if (success) {
                log.info { "Upgrade HTTP2 success. id: $id" }

                val http2Connection = Http2ClientConnection(config, tcpConnection, priorKnowledge = false)
                val response = http2Connection.initH2cAndReceiveResponse(
                    message.contentHandler,
                    message.expectServerAcceptsContent,
                    buffer
                )
                http2ClientConnection = http2Connection
                httpVersion = HttpVersion.HTTP_2
                return response.await().also {
                    log.info { "Init HTTP2 connection and receive the response success. id: $id" }
                }
            } else {
                if (buffer != null) {
                    while (buffer.hasRemaining()) {
                        val end = parser.parseNext(buffer)
                        if (end || parser.isState(HttpParser.State.END)) break
                    }
                }
                parser.parseAll(tcpConnection)
                return handler.complete()
            }
        } else {
            parser.parseAll(tcpConnection)
            return handler.complete()
        }
    }

    private fun isUpgradeToHttp2Success(): Boolean {
        return httpVersion == HttpVersion.HTTP_2 && http2ClientConnection != null
    }

    private suspend fun serverAccepted(): Boolean {
        parser.parse(tcpConnection, Predicate { it.ordinal >= HttpParser.State.HEADER.ordinal })
        return handler.serverAccepted()
    }

    private suspend fun generateRequestAndFlushData(requestMessage: RequestMessage) {
        var accepted = false
        genLoop@ while (true) {
            when (generator.state) {
                START -> generateHeader(requestMessage)
                COMMITTED -> {
                    if (requestMessage.expectServerAcceptsContent) {
                        if (accepted) {
                            generateContent(requestMessage)
                        } else {
                            if (serverAccepted()) {
                                accepted = true
                                parser.reset()
                                generateContent(requestMessage)
                                log.debug("HTTP1 client receives 100 continue and generates content complete. id: $id")
                            } else {
                                requestMessage.contentProvider?.closeFuture()?.await()
                                break@genLoop
                            }
                        }
                    } else generateContent(requestMessage)
                }
                COMPLETING -> completeContent()
                END -> {
                    tcpConnection.flush()
                    requestMessage.contentProvider?.closeFuture()?.await()
                    break@genLoop
                }
                else -> throw IllegalStateException("The HTTP client generator state error. ${generator.state}")
            }
        }
    }

    private suspend fun generateHeader(requestMessage: RequestMessage) {
        val hasContent = (requestMessage.contentProvider != null)
        generator.generateRequest(requestMessage.request, headerBuffer, null, null, !hasContent)
            .assert(FLUSH)
        flushHeaderBuffer()
    }

    private suspend fun generateContent(requestMessage: RequestMessage) {
        requireNotNull(requestMessage.contentProvider)

        val pos = contentBuffer.flipToFill()
        val len = requestMessage.contentProvider.read(contentBuffer).await()
        contentBuffer.flipToFlush(pos)

        val last = (len == -1)

        if (generator.isChunking) {
            when (val result = generator.generateRequest(null, null, chunkBuffer, contentBuffer, last)) {
                FLUSH -> flushChunkedContentBuffer()
                CONTINUE -> { // ignore the generator result continue.
                }
                SHUTDOWN_OUT, DONE -> completeContent()
                else -> throw IllegalStateException("The HTTP client generator result error. $result")
            }
        } else {
            when (val result = generator.generateRequest(null, null, null, contentBuffer, last)) {
                FLUSH -> flushContentBuffer()
                CONTINUE -> { // ignore the generator result continue.
                }
                SHUTDOWN_OUT, DONE -> completeContent()
                else -> throw IllegalStateException("The HTTP client generator result error. $result")
            }
        }
    }

    private suspend fun completeContent() {
        if (generator.isChunking) {
            when (val result = generator.generateRequest(null, null, chunkBuffer, null, true)) {
                FLUSH -> flushChunkBuffer()
                CONTINUE, SHUTDOWN_OUT, DONE -> { // ignore the generator result done.
                }
                else -> throw IllegalStateException("The HTTP client generator result error. $result")
            }
        } else {
            generator.generateRequest(null, null, null, null, true)
                .assert(setOf(DONE, SHUTDOWN_OUT, CONTINUE))
        }
    }

    private suspend fun flushHeaderBuffer() {
        if (headerBuffer.hasRemaining()) {
            val size = tcpConnection.write(headerBuffer).await()
            log.debug { "flush header bytes: $size" }
        }
        BufferUtils.clear(headerBuffer)
    }

    private suspend fun flushContentBuffer() {
        if (contentBuffer.hasRemaining()) {
            val size = tcpConnection.write(contentBuffer).await()
            log.debug { "flush content bytes: $size" }
        }
        BufferUtils.clear(contentBuffer)
    }

    private suspend fun flushChunkedContentBuffer() {
        val bufArray = arrayOf(chunkBuffer, contentBuffer)
        val remaining = bufArray.map { it.remaining().toLong() }.sum()
        if (remaining > 0) {
            val size = tcpConnection.write(bufArray, 0, bufArray.size).await()
            log.debug { "flush chunked content bytes: $size" }
        }
        bufArray.forEach(BufferUtils::clear)
    }

    private suspend fun flushChunkBuffer() {
        if (chunkBuffer.hasRemaining()) {
            val size = tcpConnection.write(chunkBuffer).await()
            log.debug { "flush chunked bytes: $size" }
        }
        BufferUtils.clear(chunkBuffer)
    }

    override fun getHttpVersion(): HttpVersion = httpVersion

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun getTcpConnection(): TcpConnection = tcpConnection

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        return when (httpVersion) {
            HttpVersion.HTTP_2 -> sendRequestViaHttp2(request)
            HttpVersion.HTTP_1_1 -> sendRequestViaHttp1(request)
            else -> throw IllegalStateException("HTTP version not support. $httpVersion")
        }
    }

    private fun sendRequestViaHttp1(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        prepareHttp1Headers(request)
        val future = CompletableFuture<HttpClientResponse>()
        requestChannel.offer(RequestMessage(request, future))
        return future
    }

    private fun sendRequestViaHttp2(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        val http2Connection = http2ClientConnection
        requireNotNull(http2Connection)
        return http2Connection.send(request)
    }

    private data class RequestMessage(
        val httpClientRequest: HttpClientRequest,
        val response: CompletableFuture<HttpClientResponse>,
        val request: MetaData.Request = toMetaDataRequest(httpClientRequest),
        val contentProvider: HttpClientContentProvider? = httpClientRequest.contentProvider,
        val contentHandler: HttpClientContentHandler = httpClientRequest.contentHandler,
        val expectServerAcceptsContent: Boolean = httpClientRequest.httpFields.expectServerAcceptsContent(),
        val expectUpgradeHttp2: Boolean = expectUpgradeHttp2(httpClientRequest)
    )

}

fun prepareHttp1Headers(request: HttpClientRequest) {
    if (request.httpFields.getValuesList(HttpHeader.HOST.value).isEmpty()) {
        request.httpFields.put(HttpHeader.HOST, request.uri.host)
    }

    val connectionValues = request.httpFields.getCSV(HttpHeader.CONNECTION, false)
    if (connectionValues.isNotEmpty()) {
        if (!connectionValues.contains(HttpHeaderValue.KEEP_ALIVE.value)) {
            val newValues = mutableListOf<String>()
            newValues.addAll(connectionValues)
            newValues.add(HttpHeaderValue.KEEP_ALIVE.value)
            request.httpFields.remove(HttpHeader.CONNECTION)
            request.httpFields.addCSV(HttpHeader.CONNECTION, *newValues.toTypedArray())
        }
    } else {
        request.httpFields.put(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE.value)
    }
}