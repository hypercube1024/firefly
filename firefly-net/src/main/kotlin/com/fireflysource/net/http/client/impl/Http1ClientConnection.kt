package com.fireflysource.net.http.client.impl

import com.fireflysource.common.codec.base64.Base64Utils
import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.common.coroutine.consumeAll
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.io.useAwait
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.Connection
import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientContentProvider
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.expectUpgradeHttp2
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.expectUpgradeWebsocket
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.isUpgradeSuccess
import com.fireflysource.net.http.client.impl.exception.UnhandledRequestException
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.TcpBasedHttpConnection
import com.fireflysource.net.http.common.exception.Http1GeneratingResultException
import com.fireflysource.net.http.common.exception.NotSupportHttpVersionException
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.common.v1.decoder.HttpParser
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadLocalRandom

class Http1ClientConnection(
    private val config: HttpConfig,
    private val tcpConnection: TcpConnection
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, TcpBasedHttpConnection,
    AbstractHttpClientConnection {

    companion object {
        private val log = SystemLogger.create(Http1ClientConnection::class.java)
    }

    private val generator = HttpGenerator()

    private val headerBuffer = BufferUtils.allocateDirect(config.headerBufferSize)
    private val contentBuffer: ByteBuffer by lazy(LazyThreadSafetyMode.NONE) { BufferUtils.allocateDirect(config.contentBufferSize) }
    private val chunkBuffer: ByteBuffer by lazy(LazyThreadSafetyMode.NONE) { BufferUtils.allocateDirect(HttpGenerator.CHUNK_SIZE) }

    private val handler = Http1ClientResponseHandler()
    private val parser = HttpParser(handler)
    private val requestChannel = Channel<Http1ClientConnectionMessage>(Channel.UNLIMITED)

    @Volatile
    private var httpVersion: HttpVersion = HttpVersion.HTTP_1_1

    @Volatile
    private var http2ClientConnection: Http2ClientConnection? = null

    private var upgradeWebSocketSuccess: Boolean = false

    init {
        handleMessage()
    }

    private fun handleMessage() = coroutineScope.launch {
        while (true) {
            when (val message = requestChannel.receive()) {
                is RequestMessage -> {
                    val exit = handleRequest(message)
                    if (exit) {
                        break
                    }
                }
                is Stop -> break
            }
        }
    }.invokeOnCompletion { e ->
        if (e != null) {
            log.info { "The HTTP1 message job completion exception. id: $id info: ${e.javaClass.name} ${e.message}" }
        } else {
            log.info("The HTTP1 message job completion. id: $id")
        }
        processUnhandledRequest()
    }

    private suspend fun handleRequest(message: RequestMessage): Boolean {
        return try {
            log.debug {
                """
                    |handle http1 request:
                    |${message.request.method} ${message.request.uri} ${message.request.httpVersion}
                    |${message.request.fields}
                """.trimMargin()
            }

            handler.init(
                message.httpClientRequest.headerComplete,
                message.contentHandler,
                message.expectServerAcceptsContent,
                message.isHttpTunnel
            )
            if (message.expectServerAcceptsContent) {
                // flush content data after the server response 100 continue.
                generateRequestAndWaitServerAccept(message)
                waitResponse(message)
            } else {
                // avoid the request can not response the server error code if the I/O exception happened during the client sends data.
                val result = coroutineScope.async { waitResponse(message) }
                generateRequest(message)
                result.await()
            }
        } catch (e: IOException) {
            log.info { "The TCP connection IO exception. id: $id info: ${e.javaClass.name} ${e.message}" }
            completeResponseExceptionally(message, e)
            true
        } catch (e: Exception) {
            log.error { "HTTP1 client handler exception. id: $id info: ${e.javaClass.name} ${e.message}" }
            completeResponseExceptionally(message, e)
            true
        } finally {
            handler.reset()
            parser.reset()
            generator.reset()
        }
    }

    private fun processUnhandledRequest() {
        when {
            isUpgradeToHttp2Success() -> requestChannel.consumeAll { message ->
                if (message is RequestMessage && !message.response.isDone) {
                    log.info { "Client sends remaining request via HTTP2 protocol. id: $id, path: ${message.httpClientRequest.uri.path}" }
                    val future = message.response
                    sendRequestViaHttp2(message.httpClientRequest)
                        .thenAccept { future.complete(it) }
                        .exceptionallyAccept { future.completeExceptionally(it) }
                }
            }
            else -> requestChannel.consumeAll { message ->
                if (message is RequestMessage && !message.response.isDone) {
                    message.response.completeExceptionally(
                        UnhandledRequestException(
                            "The HTTP1 connection has closed. This request does not send."
                        )
                    )
                }
            }
        }
    }

    private fun completeResponseExceptionally(message: RequestMessage, e: Exception) {
        if (!message.response.isDone) {
            message.response.completeExceptionally(e)
        }
        closeAsync()
    }

    private suspend fun waitResponse(message: RequestMessage): Boolean {
        val response = parseResponse(message)
        val isNonPersistence = complete(response, message)
        return when {
            isNonPersistence -> true
            message.expectUpgradeHttp2 && isUpgradeToHttp2Success() -> true
            message.expectUpgradeWebSocket && upgradeWebSocketSuccess -> true
            message.isHttpTunnel -> true
            else -> false
        }
    }

    private suspend fun complete(response: HttpClientResponse, message: RequestMessage): Boolean {
        val request = message.request
        val isCloseConnection =
            response.httpFields.isCloseConnection(response.httpVersion) || request.fields.isCloseConnection(request.httpVersion)
        val isNonPersistence = !message.isHttpTunnel && isCloseConnection
        if (isNonPersistence) {
            this.useAwait { message.response.complete(response) }
            log.debug { "HTTP1 connection closed. id: $id, closed: ${this.isClosed}" }
        } else {
            message.response.complete(response)
        }
        return isNonPersistence
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

    private suspend fun waitServerResponse100Continue(): Boolean {
        val accepted = try {
            withTimeout(Duration.ofSeconds(config.waitResponse100ContinueTimeout).toMillis()) {
                parser.parseAll(tcpConnection)
                handler.isServerAcceptedContent()
            }
        } catch (e: TimeoutCancellationException) {
            log.info { "Wait server response 100 continue timeout. The client will send data." }
            true
        } catch (e: Exception) {
            log.error { "Wait server response 100 continue failure. The client will not send data. id: $id info: ${e.javaClass.name} ${e.message}" }
            false
        }
        if (accepted) {
            parser.reset()
        }
        return accepted
    }

    private suspend fun generateRequestAndWaitServerAccept(requestMessage: RequestMessage) {
        var accepted = false
        generateRequestLoop@ while (true) {
            when (generator.state) {
                START -> generateHeader(requestMessage)
                COMMITTED -> {
                    if (accepted) {
                        generateContent(requestMessage)
                    } else {
                        if (waitServerResponse100Continue()) {
                            accepted = true
                            generateContent(requestMessage)
                            log.debug("HTTP1 client receives 100 continue and generates content complete. id: $id")
                        } else {
                            requestMessage.contentProvider?.closeAsync()?.await()
                            break@generateRequestLoop
                        }
                    }
                }
                COMPLETING -> completeContent()
                END -> {
                    completeRequest(requestMessage)
                    break@generateRequestLoop
                }
                else -> throw Http1GeneratingResultException("The HTTP client generator state error. ${generator.state}")
            }
        }
    }

    private suspend fun generateRequest(requestMessage: RequestMessage) {
        generateRequestLoop@ while (true) {
            when (generator.state) {
                START -> generateHeader(requestMessage)
                COMMITTED -> generateContent(requestMessage)
                COMPLETING -> completeContent()
                END -> {
                    completeRequest(requestMessage)
                    break@generateRequestLoop
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

    private suspend fun completeRequest(requestMessage: RequestMessage) {
        tcpConnection.flush().await()
        requestMessage.contentProvider?.closeAsync()?.await()
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
        val remaining = bufArray.sumOf { it.remaining().toLong() }
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
        if (config.isAutoGeneratedClientHttp1Headers) {
            prepareHttp1Headers(request) { this.remoteAddress.hostName }
        }
        val future = CompletableFuture<HttpClientResponse>()
        requestChannel.trySend(RequestMessage(request, future))
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
        requestChannel.trySend(message)
        return websocketFuture
    }

    fun dispose() {
        requestChannel.trySend(Stop)
    }

    private fun genRandomWebSocketKey(): String {
        val bytes = ByteArray(16)
        ThreadLocalRandom.current().nextBytes(bytes)
        return String(Base64Utils.encode(bytes))
    }

    sealed interface Http1ClientConnectionMessage

    private data class RequestMessage(
        val httpClientRequest: HttpClientRequest,
        val response: CompletableFuture<HttpClientResponse>,
        val request: MetaData.Request = toMetaDataRequest(httpClientRequest),
        val contentProvider: HttpClientContentProvider? = httpClientRequest.contentProvider,
        val contentHandler: HttpClientContentHandler = httpClientRequest.contentHandler,
        val expectServerAcceptsContent: Boolean = httpClientRequest.httpFields.expectServerAcceptsContent(),
        val expectUpgradeHttp2: Boolean = expectUpgradeHttp2(httpClientRequest),
        val expectUpgradeWebSocket: Boolean = expectUpgradeWebsocket(httpClientRequest),
        val isHttpTunnel: Boolean = httpClientRequest.method.equals(HttpMethod.CONNECT.value),
        val webSocketClientConnection: CompletableFuture<WebSocketConnection>? = null,
        val webSocketClientRequest: WebSocketClientRequest? = null
    ) : Http1ClientConnectionMessage

    private object Stop : Http1ClientConnectionMessage
}

fun prepareHttp1Headers(request: HttpClientRequest, defaultHost: () -> String) {
    if (request.httpFields.getValuesList(HttpHeader.HOST.value).isEmpty()) {
        val host = if (request.uri.host.isNullOrBlank()) defaultHost.invoke() else request.uri.host
        request.httpFields.put(HttpHeader.HOST, host)
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