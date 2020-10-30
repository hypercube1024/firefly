package com.fireflysource.net.http.server.impl

import com.fireflysource.common.codec.base64.Base64Utils
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.common.v2.decoder.SettingsBodyParser
import com.fireflysource.net.http.common.v2.frame.SettingsFrame
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.RoutingContext
import com.fireflysource.net.http.server.impl.router.AsyncRoutingContext
import com.fireflysource.net.websocket.common.impl.AsyncWebSocketConnection
import com.fireflysource.net.websocket.common.model.AcceptHash
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class Http1ServerRequestHandler(private val connection: Http1ServerConnection) : HttpParser.RequestHandler {

    companion object {
        private val log = SystemLogger.create(Http1ServerRequestHandler::class.java)
    }

    var connectionListener: HttpServerConnection.Listener = HttpServerConnection.EMPTY_LISTENER
    private val parserChannel: Channel<ParserMessage> = Channel(Channel.UNLIMITED)
    private var expectUpgradeHttp2 = false
    private var settingsFrame: SettingsFrame? = null
    private var expectUpgradeWebsocket = false

    init {
        handleParserMessageJob()
    }

    private fun handleParserMessageJob() = connection.coroutineScope.launch {
        var request: MetaData.Request? = null
        var context: AsyncRoutingContext? = null
        parserLoop@ while (true) {
            val message = parserChannel.receive()
            try {
                when (message) {
                    is StartRequest -> request = newRequest(message)
                    is ParsedHeader -> addHeader(request, message)
                    is HeaderComplete -> context = newContextAndNotifyHeaderComplete(request)
                    is Content -> acceptContent(context, message)
                    is ContentComplete -> closeContentHandler(context)
                    is MessageComplete -> notifyHttpRequestComplete(context)
                    is BadMessage -> notifyException(request, context, message.exception)
                    is EarlyEOF -> notifyException(request, context, IllegalStateException("Parser early EOF"))
                    is EndRequestHandler -> {
                        log.info { "Exit the server request handler. id: ${connection.id}" }
                        break@parserLoop
                    }
                }
            } catch (e: Exception) {
                log.error(e) { "Handle HTTP1 server parser message exception." }
                notifyException(request, context, e)
            }
        }
    }

    private fun newRequest(message: StartRequest): MetaData.Request {
        return MetaData.Request(message.method, HttpURI(message.uri), message.version, HttpFields())
    }

    private fun addHeader(request: MetaData.Request?, message: ParsedHeader) {
        requireNotNull(request)
        request.fields.add(message.field)
    }

    private suspend fun newContextAndNotifyHeaderComplete(request: MetaData.Request?): AsyncRoutingContext {
        requireNotNull(request)
        val httpServerRequest = AsyncHttpServerRequest(request, connection.config)

        this.expectUpgradeHttp2 = HttpProtocolNegotiator.expectUpgradeHttp2(httpServerRequest)
        if (expectUpgradeHttp2) {
            val settingsBody = Base64Utils.decodeFromUrlSafeString(request.fields[HttpHeader.HTTP2_SETTINGS])
            val settings = SettingsBodyParser.parseBody(ByteBuffer.wrap(settingsBody))
            this.settingsFrame = settings
            this.expectUpgradeHttp2 = settings != null
        }
        this.expectUpgradeWebsocket = HttpProtocolNegotiator.expectUpgradeWebsocket(httpServerRequest)


        val ctx = newContext(httpServerRequest)
        notifyHeaderComplete(ctx)
        return ctx
    }

    private fun newContext(request: MetaData.Request?): AsyncRoutingContext? {
        return if (request != null) {
            val httpServerRequest = AsyncHttpServerRequest(request, connection.config)
            newContext(httpServerRequest)
        } else null
    }

    private fun newContext(request: AsyncHttpServerRequest): AsyncRoutingContext {
        val expect100 = request.httpFields.expectServerAcceptsContent()
        val closeConnection = request.httpFields.isCloseConnection(request.httpVersion)
        return AsyncRoutingContext(
            request,
            Http1ServerResponse(connection, expect100, closeConnection),
            connection
        )
    }

    private suspend fun notifyHeaderComplete(context: RoutingContext) {
        connectionListener.onHeaderComplete(context).await()
    }

    private fun acceptContent(context: AsyncRoutingContext?, message: Content) {
        requireNotNull(context)
        context.request.contentHandler.accept(message.byteBuffer, context)
    }

    private suspend fun closeContentHandler(context: AsyncRoutingContext?) {
        requireNotNull(context)
        context.request.contentHandler.closeFuture().await()
    }

    private suspend fun notifyHttpRequestComplete(context: RoutingContext?) {
        requireNotNull(context)
        context.request.isRequestComplete = true
        if (expectUpgradeHttp2 && !context.response.isCommitted) {
            val settings = settingsFrame
            requireNotNull(settings)

            switchingHttp2Response()
            val http2ServerConnection = createHttp2Connection()
            val stream = http2ServerConnection.upgradeHttp2(settings)
            val response = Http2ServerResponse(http2ServerConnection, stream)
            val http2Context = AsyncRoutingContext(
                context.request,
                response,
                http2ServerConnection
            )

            connection.notifyUpgradeProtocol(true)
            endRequestHandler()
            connectionListener.onHttpRequestComplete(http2Context).await()
        } else if (expectUpgradeWebsocket && !context.response.isCommitted) {
            switchingWebSocket(context)

            connection.notifyUpgradeProtocol(true)
            endRequestHandler()
        } else {
            connection.notifyUpgradeProtocol(false)
            connectionListener.onHttpRequestComplete(context).await()
        }
        log.debug { "HTTP1 server handles request success. id: ${connection.id}" }
    }

    private suspend fun switchingWebSocket(ctx: RoutingContext) {
        val handler = connectionListener.onWebSocketHandshake(ctx).await()

        val clientKey = ctx.httpFields[HttpHeader.SEC_WEBSOCKET_KEY]
        val serverAccept = AcceptHash.hashKey(clientKey)

        val clientExtensions = ctx.httpFields.getValuesList(HttpHeader.SEC_WEBSOCKET_EXTENSIONS)
        val serverExtensions = handler.extensionSelector.select(clientExtensions)

        val clientSubProtocols = ctx.httpFields.getValuesList(HttpHeader.SEC_WEBSOCKET_SUBPROTOCOL)
        val serverSubProtocols = handler.subProtocolSelector.select(clientSubProtocols)

        val message = buildString {
            append("HTTP/1.1 101 Switching Protocols\r\n")
            append("Connection: Upgrade\r\n")
            append("Upgrade: websocket\r\n")
            append("${HttpHeader.SEC_WEBSOCKET_ACCEPT.value}: ${serverAccept}\r\n")
            if (!serverExtensions.isNullOrEmpty()) {
                append("${HttpHeader.SEC_WEBSOCKET_EXTENSIONS.value}: ${serverExtensions.joinToString(", ")}\r\n")
            }
            if (!serverSubProtocols.isNullOrEmpty()) {
                append("${HttpHeader.SEC_WEBSOCKET_SUBPROTOCOL.value}: ${serverSubProtocols.joinToString(", ")}\r\n")
            }
            append("\r\n")
        }
        connection.tcpConnection.write(BufferUtils.toBuffer(message)).await()
        connection.tcpConnection.flush().await()
        log.info { "Server response 101 Switching Protocols. upgrade: websocket, id: ${connection.id}" }

        val webSocketConnection = AsyncWebSocketConnection(
            connection.tcpConnection,
            handler.policy,
            handler.url,
            serverExtensions ?: listOf(),
            AsyncWebSocketConnection.defaultExtensionFactory,
            serverSubProtocols ?: listOf()
        )
        webSocketConnection.setWebSocketMessageHandler(handler.messageHandler)
        webSocketConnection.begin()
        handler.connectionListener.accept(webSocketConnection).await()
    }

    private fun createHttp2Connection() =
        Http2ServerConnection(connection.config, connection.tcpConnection)
            .also { it.setListener(connectionListener).begin() }

    private suspend fun switchingHttp2Response() {
        val message = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Connection: Upgrade\r\n" +
                "Upgrade: h2c\r\n" +
                "\r\n"
        connection.tcpConnection.write(BufferUtils.toBuffer(message)).await()
        connection.tcpConnection.flush().await()
        log.info { "Server response 101 Switching Protocols. upgrade: h2c, id: ${connection.id}" }
    }

    private fun endRequestHandler() {
        parserChannel.offer(EndRequestHandler)
    }

    private suspend fun notifyException(request: MetaData.Request?, context: RoutingContext?, exception: Throwable) {
        val ctx = context ?: newContext(request)
        try {
            connection.notifyUpgradeProtocol(false)
            log.error(exception) { "HTTP1 server parser exception. id: ${connection.id}" }
            connectionListener.onException(ctx, exception).await()
        } catch (e: Exception) {
            log.error(e) { "HTTP1 server on exception. id: ${connection.id}" }
        }
        if (ctx == null) {
            connection.closeFuture()
        }
    }

    override fun startRequest(method: String, uri: String, version: HttpVersion): Boolean {
        parserChannel.offer(StartRequest(method, uri, version))
        return false
    }

    override fun getHeaderCacheSize(): Int = 4096

    override fun parsedHeader(field: HttpField) {
        parserChannel.offer(ParsedHeader(field))
    }

    override fun headerComplete(): Boolean {
        parserChannel.offer(HeaderComplete)
        return false
    }

    override fun content(byteBuffer: ByteBuffer): Boolean {
        parserChannel.offer(Content(byteBuffer))
        return false
    }

    override fun contentComplete(): Boolean {
        parserChannel.offer(ContentComplete)
        return false
    }

    override fun messageComplete(): Boolean {
        parserChannel.offer(MessageComplete)
        return true
    }

    override fun earlyEOF() {
        parserChannel.offer(EarlyEOF)
    }

    override fun badMessage(failure: BadMessageException) {
        parserChannel.offer(BadMessage(failure))
    }

}

sealed class ParserMessage
data class StartRequest(val method: String, val uri: String, val version: HttpVersion) : ParserMessage()
data class ParsedHeader(val field: HttpField) : ParserMessage()
object HeaderComplete : ParserMessage()
class Content(val byteBuffer: ByteBuffer) : ParserMessage()
object ContentComplete : ParserMessage()
object MessageComplete : ParserMessage()
object EarlyEOF : ParserMessage()
class BadMessage(val exception: Exception) : ParserMessage()
object EndRequestHandler : ParserMessage()