package com.fireflysource.net.http.client.impl

import com.fireflysource.common.codec.base64.Base64Utils
import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.common.coroutine.pollAll
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.io.useAwait
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.Connection
import com.fireflysource.net.http.client.*
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.expectUpgradeHttp2
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.expectUpgradeWebsocket
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.isUpgradeSuccess
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.TcpBasedHttpConnection
import com.fireflysource.net.http.common.exception.Http1GeneratingResultException
import com.fireflysource.net.http.common.exception.NotSupportHttpVersionException
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
import com.fireflysource.net.websocket.client.WebSocketClientRequest
import com.fireflysource.net.websocket.common.WebSocketConnection
import com.fireflysource.net.websocket.common.exception.UpgradeWebSocketConnectionException
import com.fireflysource.net.websocket.common.impl.AsyncWebSocketConnection
import com.fireflysource.net.websocket.common.model.AcceptHash
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadLocalRandom


class Http1ClientConnection(
    private val config: HttpConfig,
    private val tcpConnection: TcpConnection
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, TcpBasedHttpConnection, HttpClientConnection {

    companion object {
        private val log = SystemLogger.create(Http1ClientConnection::class.java)
    }

    private val generator = HttpGenerator()

    private val headerBuffer: ByteBuffer by lazy(LazyThreadSafetyMode.NONE) { BufferUtils.allocateDirect(config.headerBufferSize) }
    private val contentBuffer: ByteBuffer by lazy(LazyThreadSafetyMode.NONE) { BufferUtils.allocateDirect(config.contentBufferSize) }
    private val chunkBuffer: ByteBuffer by lazy(LazyThreadSafetyMode.NONE) { BufferUtils.allocateDirect(HttpGenerator.CHUNK_SIZE) }

    private val handler = Http1ClientResponseHandler()
    private val parser = HttpParser(handler)
    private val requestChannel = Channel<RequestMessage>(Channel.UNLIMITED)
    private var unhandledRequestMessage: (HttpClientRequest, CompletableFuture<HttpClientResponse>) -> Unit =
        { _, future ->
            future.completeExceptionally(IllegalStateException("The HTTP1 connection has closed."))
        }

    @Volatile
    private var httpVersion: HttpVersion = HttpVersion.HTTP_1_1

    @Volatile
    private var http2ClientConnection: Http2ClientConnection? = null

    private var upgradeWebSocketSuccess: Boolean = false

    init {
        handleRequestMessage()
    }

    fun onUnhandledRequestMessage(block: (HttpClientRequest, CompletableFuture<HttpClientResponse>) -> Unit): Http1ClientConnection {
        this.unhandledRequestMessage = block
        return this
    }

    private fun handleRequestMessage() = coroutineScope.launch {
        handleRequestLoop@ while (true) {
            val message = requestChannel.receive()
            try {
                log.debug {
                    """
                    |handle http1 request:
                    |${message.request.method} ${message.request.uri} ${message.request.httpVersion}
                    |${message.request.fields}
                """.trimMargin()
                }

                handler.init(message.contentHandler, message.expectServerAcceptsContent)
                generateRequestAndFlushData(message)
                log.debug("HTTP1 client generates request complete. id: $id")
                val closed = parseResponse(message).complete(message)
                if (closed) {
                    break@handleRequestLoop
                }
                if (message.expectUpgradeHttp2 && isUpgradeToHttp2Success()) {
                    break@handleRequestLoop
                }
                if (message.expectUpgradeWebSocket && upgradeWebSocketSuccess) {
                    break@handleRequestLoop
                }
            } catch (e: IOException) {
                log.info { "The TCP connection IO exception. message: ${e.message}. id: $id" }
                completeResponseExceptionally(message, e)
                break@handleRequestLoop
            } catch (e: Exception) {
                log.error(e) { "HTTP1 client handler exception. id: $id" }
                completeResponseExceptionally(message, e)
                break@handleRequestLoop
            } finally {
                handler.reset()
                parser.reset()
                generator.reset()
            }
        }
    }.invokeOnCompletion { cause ->
        if (cause != null) {
            log.info { "The HTTP1 request message job completion. cause: ${cause.message}" }
        }
        when {
            isUpgradeToHttp2Success() -> requestChannel.pollAll { message ->
                log.info { "Client sends remaining request via HTTP2 protocol. id: $id, path: ${message.httpClientRequest.uri.path}" }
                val future = message.response
                sendRequestViaHttp2(message.httpClientRequest)
                    .thenAccept { future.complete(it) }
                    .exceptionallyAccept { future.completeExceptionally(it) }
            }
            else -> requestChannel.pollAll { message ->
                unhandledRequestMessage(message.httpClientRequest, message.response)
            }
        }
    }

    private fun completeResponseExceptionally(message: RequestMessage, e: Exception) {
        if (!message.response.isDone) {
            message.response.completeExceptionally(e)
        }
        closeFuture()
    }

    private suspend fun HttpClientResponse.complete(message: RequestMessage): Boolean {
        val request = message.request
        val response = this
        val closed = response.httpFields.isCloseConnection(response.httpVersion)
                || request.fields.isCloseConnection(request.httpVersion)
        if (closed) {
            this@Http1ClientConnection.useAwait { message.response.complete(response) }
            log.debug { "HTTP1 connection closed. id: $id, closed: ${this@Http1ClientConnection.isClosed}" }
        } else message.response.complete(response)
        return closed
    }

    private suspend fun parseResponse(message: RequestMessage): HttpClientResponse {
        val remainingData = parser.parseAll(tcpConnection)
        val response = handler.complete()
        return when {
            message.expectUpgradeHttp2 -> upgradeHttp2Connection(response, message, remainingData)
            message.expectUpgradeWebSocket -> upgradeWebSocketConnection(response, message, remainingData)
            else -> response
        }
    }

    private fun upgradeWebSocketConnection(
        response: HttpClientResponse,
        message: RequestMessage,
        remainingData: ByteBuffer?
    ): HttpClientResponse {
        if (response.status != HttpStatus.SWITCHING_PROTOCOLS_101) {
            val e =
                UpgradeWebSocketConnectionException("The upgrade response status is not 101. status: ${response.status}")
            message.webSocketClientConnection?.completeExceptionally(e)
            return response
        }

        if (!response.httpFields.contains(HttpHeader.CONNECTION, "Upgrade")) {
            val e =
                UpgradeWebSocketConnectionException("The upgrade response does not contain the Connection Upgrade field.")
            message.webSocketClientConnection?.completeExceptionally(e)
            return response
        }

        if (!response.httpFields.contains(HttpHeader.UPGRADE, "websocket")) {
            val e =
                UpgradeWebSocketConnectionException("The upgrade response does not contain the UPGRADE websocket field.")
            message.webSocketClientConnection?.completeExceptionally(e)
            return response
        }

        if (!response.httpFields.contains(HttpHeader.SEC_WEBSOCKET_ACCEPT)) {
            val e =
                UpgradeWebSocketConnectionException("The upgrade response does not contain the Sec-WebSocket-Accept.")
            message.webSocketClientConnection?.completeExceptionally(e)
            return response
        }

        val clientKey = message.httpClientRequest.httpFields[HttpHeader.SEC_WEBSOCKET_KEY]
        val serverKey = AcceptHash.hashKey(clientKey)
        if (response.httpFields[HttpHeader.SEC_WEBSOCKET_ACCEPT] != serverKey) {
            val e = UpgradeWebSocketConnectionException("The upgrade response SEC_WEBSOCKET_ACCEPT is illegal.")
            message.webSocketClientConnection?.completeExceptionally(e)
            return response
        }

        log.info { "Upgrade websocket. Client received 101 Switching Protocols. id: $id" }
        val webSocketClientRequest = message.webSocketClientRequest
        requireNotNull(webSocketClientRequest)
        val serverExtensions = response.httpFields.getValuesList(HttpHeader.SEC_WEBSOCKET_EXTENSIONS)
        val serverSubProtocols = response.httpFields.getValuesList(HttpHeader.SEC_WEBSOCKET_SUBPROTOCOL)
        val webSocketConnection = AsyncWebSocketConnection(
            tcpConnection,
            webSocketClientRequest.policy,
            webSocketClientRequest.url,
            serverExtensions ?: listOf(),
            AsyncWebSocketConnection.defaultExtensionFactory,
            serverSubProtocols ?: listOf(),
            remainingData = remainingData
        )
        webSocketConnection.setWebSocketMessageHandler(webSocketClientRequest.handler)
        webSocketConnection.begin()
        upgradeWebSocketSuccess = true
        message.webSocketClientConnection?.complete(webSocketConnection)
        return response
    }

    private suspend fun upgradeHttp2Connection(
        response: HttpClientResponse,
        message: RequestMessage,
        remainingData: ByteBuffer?
    ): HttpClientResponse {
        return if (isUpgradeSuccess(response)) {
            log.info { "Upgrade HTTP2. Client received 101 Switching Protocols. id: $id" }

            val http2Connection = Http2ClientConnection(config, tcpConnection, priorKnowledge = false)
            val responseFuture =
                http2Connection.upgradeHttp2(message.httpClientRequest, remainingData)
            http2ClientConnection = http2Connection
            httpVersion = HttpVersion.HTTP_2
            responseFuture.await().also { log.info { "Client upgrades HTTP2 success. id: $id" } }
        } else response.also { log.info { "Client upgrades HTTP2 failure. id: $id" } }
    }


    private fun isUpgradeToHttp2Success(): Boolean {
        return httpVersion == HttpVersion.HTTP_2 && http2ClientConnection != null
    }

    private suspend fun serverAccepted(): Boolean {
        parser.parse(tcpConnection) { it.ordinal >= HttpParser.State.HEADER.ordinal }
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
                else -> throw Http1GeneratingResultException("The HTTP client generator state error. ${generator.state}")
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
                else -> throw Http1GeneratingResultException("The HTTP client generator result error. $result")
            }
        } else {
            when (val result = generator.generateRequest(null, null, null, contentBuffer, last)) {
                FLUSH -> flushContentBuffer()
                CONTINUE -> { // ignore the generator result continue.
                }
                SHUTDOWN_OUT, DONE -> completeContent()
                else -> throw Http1GeneratingResultException("The HTTP client generator result error. $result")
            }
        }
    }

    private suspend fun completeContent() {
        if (generator.isChunking) {
            when (val result = generator.generateRequest(null, null, chunkBuffer, null, true)) {
                FLUSH -> flushChunkBuffer()
                CONTINUE, SHUTDOWN_OUT, DONE -> { // ignore the generator result done.
                }
                else -> throw Http1GeneratingResultException("The HTTP client generator result error. $result")
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
            else -> throw NotSupportHttpVersionException("HTTP version not support. $httpVersion")
        }
    }

    private fun sendRequestViaHttp1(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        log.debug { "Send request via HTTP1 protocol. id: $id" }
        prepareHttp1Headers(request)
        val future = CompletableFuture<HttpClientResponse>()
        requestChannel.offer(RequestMessage(request, future))
        return future
    }

    private fun sendRequestViaHttp2(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        val http2Connection = http2ClientConnection
        requireNotNull(http2Connection)
        log.debug { "Send request via HTTP2 protocol. id: $id" }
        return http2Connection.send(request)
    }

    fun upgradeWebSocket(webSocketClientRequest: WebSocketClientRequest): CompletableFuture<WebSocketConnection> {
        val request = AsyncHttpClientRequest()
        request.method = HttpMethod.GET.value
        request.uri = HttpURI(webSocketClientRequest.url)
        request.httpFields = HttpFields()
        request.httpFields.put(HttpHeader.HOST, request.uri.host)
        request.httpFields.put(HttpHeader.CONNECTION, "Upgrade")
        request.httpFields.put(HttpHeader.UPGRADE, "websocket")
        request.httpFields.put(HttpHeader.SEC_WEBSOCKET_VERSION, "13")
        request.httpFields.put(HttpHeader.SEC_WEBSOCKET_KEY, genRandomWebSocketKey())
        if (!webSocketClientRequest.extensions.isNullOrEmpty()) {
            request.httpFields.put(
                HttpHeader.SEC_WEBSOCKET_EXTENSIONS,
                webSocketClientRequest.extensions.joinToString(", ")
            )
        }
        if (!webSocketClientRequest.subProtocols.isNullOrEmpty()) {
            request.httpFields.put(
                HttpHeader.SEC_WEBSOCKET_SUBPROTOCOL,
                webSocketClientRequest.subProtocols.joinToString(", ")
            )
        }

        val websocketFuture = CompletableFuture<WebSocketConnection>()
        val responseFuture = CompletableFuture<HttpClientResponse>()
        val message = RequestMessage(
            httpClientRequest = request,
            response = responseFuture,
            webSocketClientConnection = websocketFuture,
            webSocketClientRequest = webSocketClientRequest
        )
        requestChannel.offer(message)
        return websocketFuture
    }

    private fun genRandomWebSocketKey(): String {
        val bytes = ByteArray(16)
        ThreadLocalRandom.current().nextBytes(bytes)
        return String(Base64Utils.encode(bytes))
    }

    private data class RequestMessage(
        val httpClientRequest: HttpClientRequest,
        val response: CompletableFuture<HttpClientResponse>,
        val request: MetaData.Request = toMetaDataRequest(httpClientRequest),
        val contentProvider: HttpClientContentProvider? = httpClientRequest.contentProvider,
        val contentHandler: HttpClientContentHandler = httpClientRequest.contentHandler,
        val expectServerAcceptsContent: Boolean = httpClientRequest.httpFields.expectServerAcceptsContent(),
        val expectUpgradeHttp2: Boolean = expectUpgradeHttp2(httpClientRequest),
        val expectUpgradeWebSocket: Boolean = expectUpgradeWebsocket(httpClientRequest),
        val webSocketClientConnection: CompletableFuture<WebSocketConnection>? = null,
        val webSocketClientRequest: WebSocketClientRequest? = null
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