package com.fireflysource.net.http.client.impl

import com.fireflysource.common.`object`.Assert
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.Connection
import com.fireflysource.net.http.client.*
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.TcpBasedHttpConnection
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpHeaderValue
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator.Result.*
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator.State.*
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class Http1ClientConnection(
    private val config: HttpConfig,
    private val tcpConnection: TcpConnection
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, HttpClientConnection, TcpBasedHttpConnection {

    companion object {
        private val log = SystemLogger.create(Http1ClientConnection::class.java)
    }

    private val generator = HttpGenerator()

    // generator buffer
    private val headerBuffer = BufferUtils.allocateDirect(config.requestHeaderBufferSize)
    private val contentBuffer = BufferUtils.allocateDirect(config.contentBufferSize)
    private val chunkBuffer = BufferUtils.allocateDirect(HttpGenerator.CHUNK_SIZE)

    // parser
    private val handler = Http1ClientResponseHandler()
    private val parser = HttpParser(handler)

    private val requestChannel = Channel<RequestMessage>(Channel.UNLIMITED)
    private var upgradeHttpClientConnection: HttpClientConnection? = null
    private var sendHttp2UpgradeHeaders = AtomicBoolean(true)
    private val mutex = Mutex()


    init {
        generateRequestAndParseResponseJob()
    }

    private fun generateRequestAndParseResponseJob() = tcpConnection.coroutineScope.launch {
        while (true) {
            val requestMessage = requestChannel.receive()

            try {
                generateRequestAndFlushData(requestMessage)

                // receive response data
                handler.contentHandler = requestMessage.contentHandler
                val response = parseResponse()
                requestMessage.response.complete(response)
            } catch (e: Exception) {
                requestMessage.response.completeExceptionally(e)
            } finally {
                handler.reset()
                parser.reset()
                generator.reset()
            }
        }
    }

    private suspend fun parseResponse(): HttpClientResponse {
        Assert.state(parser.isState(HttpParser.State.START), "The parser state error. ${parser.state}")

        val inputChannel = tcpConnection.inputChannel
        recvLoop@ while (!parser.isState(HttpParser.State.END)) {
            val buffer = inputChannel.receive()

            var remaining = buffer.remaining()
            readBufLoop@ while (!parser.isState(HttpParser.State.END) && remaining > 0) {
                val wasRemaining = remaining
                parser.parseNext(buffer)
                remaining = buffer.remaining()
                Assert.state(remaining != wasRemaining, "The received data can not be consumed")
            }
        }

        return handler.toHttpClientResponse()
    }

    private suspend fun generateRequestAndFlushData(requestMessage: RequestMessage) {
        genLoop@ while (true) {
            when (generator.state) {
                START -> generateHeader(requestMessage)
                COMMITTED -> generateContent(requestMessage)
                COMPLETING -> completeContent()
                END -> {
                    requestMessage.contentProvider?.closeFuture()?.await()
                    break@genLoop
                }
                else -> throw IllegalStateException("The HTTP client generator state error. ${generator.state}")
            }
        }
    }

    private suspend fun generateHeader(requestMessage: RequestMessage) {
        val hasContent = (requestMessage.contentProvider != null)
        val result =
            generator.generateRequest(requestMessage.request, headerBuffer, null, null, !hasContent)
        Assert.state(result == FLUSH, "The HTTP client generator result error. $result")
        flushHeaderBuffer()
    }

    private suspend fun generateContent(requestMessage: RequestMessage) {
        requireNotNull(requestMessage.contentProvider)
        val pos = BufferUtils.flipToFill(contentBuffer)
        val len = requestMessage.contentProvider.read(contentBuffer).await()
        BufferUtils.flipToFlush(contentBuffer, pos)

        val last = (len == -1)

        if (generator.isChunking) {
            when (val result = generator.generateRequest(null, null, chunkBuffer, contentBuffer, last)) {
                FLUSH -> flushChunkedContentBuffer()
                CONTINUE -> { // ignore the generator result continue.
                }
                DONE -> completeContent()
                else -> throw IllegalStateException("The HTTP client generator result error. $result")
            }
        } else {
            when (val result = generator.generateRequest(null, null, null, contentBuffer, last)) {
                FLUSH -> flushContentBuffer()
                CONTINUE -> { // ignore the generator result continue.
                }
                DONE -> completeContent()
                else -> throw IllegalStateException("The HTTP client generator result error. $result")
            }
        }
    }

    private suspend fun completeContent() {
        if (generator.isChunking) {
            when (val result = generator.generateRequest(null, null, chunkBuffer, null, true)) {
                FLUSH -> flushChunkBuffer()
                CONTINUE, DONE -> { // ignore the generator result done.
                }
                else -> throw IllegalStateException("The HTTP client generator result error. $result")
            }
        } else {
            val result = generator.generateRequest(null, null, null, null, true)
            Assert.state(
                result == DONE || result == CONTINUE,
                "The HTTP client generator result error. $result"
            )
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

    fun sendRequestTryToUpgradeHttp2(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {

        fun sendRequestMaybeHttp2(): CompletableFuture<HttpClientResponse> {
            val httpConnection = upgradeHttpClientConnection
            return if (httpConnection != null) {
                log.debug { "Send request with HTTP2 protocol. id: $id" }
                httpConnection.send(request)
            } else {
                log.debug { "Send request with HTTP1 protocol. id: $id" }
                send(request)
            }
        }

        return if (sendHttp2UpgradeHeaders.get()) {
            tcpConnection.coroutineScope.async {
                mutex.withLock {
                    if (sendHttp2UpgradeHeaders.get()) {
                        sendRequestWithHttp2UpgradeHeader(request)
                    } else {
                        sendRequestMaybeHttp2().await()
                    }
                }
            }.asCompletableFuture()
        } else {
            sendRequestMaybeHttp2()
        }
    }

    private suspend fun sendRequestWithHttp2UpgradeHeader(request: HttpClientRequest): HttpClientResponse {
        log.debug { "Try to add h2c headers. id: $id" }
        // detect the protocol version using the Upgrade header
        HttpProtocolNegotiator.addHttp2UpgradeHeader(request)

        val response = send(request).await()
        sendHttp2UpgradeHeaders.set(false)

        return if (HttpProtocolNegotiator.isUpgradeSuccess(response)) {
            // switch the protocol to HTTP2
            val http2ClientConnection = Http2ClientConnection(config, tcpConnection)
            upgradeHttpClientConnection = http2ClientConnection

            log.info { "HTTP1 connection upgrades HTTP2 success. id: $id" }

            HttpProtocolNegotiator.removeHttp2UpgradeHeader(request)
            http2ClientConnection.send(request).await()
        } else {
            log.info { "HTTP1 connection upgrades HTTP2 failure. id: $id" }
            response
        }
    }

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_1_1

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun getTcpConnection(): TcpConnection = tcpConnection

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        prepareHttp1Headers(request)
        val future = CompletableFuture<HttpClientResponse>()
        val metaDataRequest = toMetaDataRequest(request)
        requestChannel.offer(RequestMessage(metaDataRequest, request.contentProvider, request.contentHandler, future))
        return future
    }

    private data class RequestMessage(
        val request: MetaData.Request,
        val contentProvider: HttpClientContentProvider?,
        val contentHandler: HttpClientContentHandler?,
        val response: CompletableFuture<HttpClientResponse>
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