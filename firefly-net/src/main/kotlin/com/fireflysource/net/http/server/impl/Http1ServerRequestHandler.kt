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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.net.URL
import java.nio.ByteBuffer

class Http1ServerRequestHandler(private val connection: Http1ServerConnection) : HttpParser.RequestHandler {

    companion object {
        private val log = SystemLogger.create(Http1ServerRequestHandler::class.java)
    }

    var connectionListener: HttpServerConnection.Listener = HttpServerConnection.EMPTY_LISTENER
    private val parserChannel: Channel<ParserMessage> = Channel(Channel.UNLIMITED)
    private var expectUpgradeHttp2 = false
    private var settingsFrame: SettingsFrame? = null
    private val upgradeHttp2Result: Channel<Boolean> = Channel(1)

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
                    is BadMessage -> notifyException(context, message.exception)
                    is EarlyEOF -> notifyException(context, IllegalStateException("Parser early EOF"))
                    is EndHandleRequest -> break@parserLoop
                }
            } catch (e: Exception) {
                log.error(e) { "Handle HTTP1 server parser message exception." }
                notifyException(context, e)
            }
        }
    }

    private fun newRequest(message: StartRequest): MetaData.Request {
        return MetaData.Request(message.method, HttpURI(URL(message.uri).toURI()), message.version, HttpFields())
    }

    private fun addHeader(request: MetaData.Request?, message: ParsedHeader) {
        requireNotNull(request)
        request.fields.add(message.field)
    }

    private suspend fun newContextAndNotifyHeaderComplete(request: MetaData.Request?): AsyncRoutingContext? {
        requireNotNull(request)
        val httpServerRequest = AsyncHttpServerRequest(request)

        this.expectUpgradeHttp2 = HttpProtocolNegotiator.expectUpgradeHttp2(httpServerRequest)
        if (expectUpgradeHttp2) {
            val settingsBody = Base64Utils.decodeFromUrlSafeString(request.fields[HttpHeader.HTTP2_SETTINGS])
            val settings = SettingsBodyParser.parseBody(ByteBuffer.wrap(settingsBody))
            this.settingsFrame = settings
            this.expectUpgradeHttp2 = settings != null
        }

        val expect100 = request.fields.expectServerAcceptsContent()
        val closeConnection = request.fields.isCloseConnection(request.httpVersion)
        val ctx = AsyncRoutingContext(
            httpServerRequest,
            Http1ServerResponse(connection, expect100, closeConnection),
            connection
        )
        notifyHeaderComplete(ctx)
        return ctx
    }

    private suspend fun notifyHeaderComplete(context: RoutingContext) {
        try {
            connectionListener.onHeaderComplete(context).await()
            log.debug { "HTTP1 server handles header complete success. id: ${connection.id}" }
        } catch (e: Exception) {
            log.error(e) { "HTTP1 server handles header complete exception. id: ${connection.id}" }
        }
    }

    private fun acceptContent(context: AsyncRoutingContext?, message: Content) {
        try {
            requireNotNull(context)
            context.request.contentHandler.accept(message.byteBuffer, context)
            log.debug { "HTTP1 server receives content success. id: ${connection.id}" }
        } catch (e: Exception) {
            log.error(e) { "HTTP1 server accepts content exception. id: ${connection.id}" }
        }
    }

    private suspend fun closeContentHandler(context: AsyncRoutingContext?) {
        try {
            requireNotNull(context)
            context.request.contentHandler.closeFuture().await()
        } catch (e: Exception) {
            log.error(e) { "HTTP1 server completes content exception. id: ${connection.id}" }
        }
    }

    private suspend fun notifyHttpRequestComplete(context: RoutingContext?) {
        try {
            requireNotNull(context)
            context.request.isRequestComplete = true
            if (expectUpgradeHttp2 && !context.response.isCommitted) {
                val settings = settingsFrame
                requireNotNull(settings)

                switchingHttp2()
                val http2ServerConnection = Http2ServerConnection(connection.config, connection.tcpConnection)
                val stream = http2ServerConnection.upgradeHttp2(settings)
                val response = Http2ServerResponse(http2ServerConnection, stream)
                val http2Context = AsyncRoutingContext(context.request, response, http2ServerConnection)
                upgradeHttp2Result.offer(true)
                parserChannel.offer(EndHandleRequest)
                connectionListener.onHttpRequestComplete(http2Context).await()
            } else {
                upgradeHttp2Result.offer(false)
                connectionListener.onHttpRequestComplete(context).await()
            }
            log.debug { "HTTP1 server handles request success. id: ${connection.id}" }
        } catch (e: Exception) {
            upgradeHttp2Result.offer(false)
            log.error(e) { "HTTP1 server handles request exception. id: ${connection.id}" }
        }
    }

    private suspend fun switchingHttp2() {
        val message = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Connection: Upgrade\r\n" +
                "Upgrade: h2c\r\n" +
                "\r\n"
        connection.tcpConnection.write(BufferUtils.toBuffer(message)).await()
    }

    suspend fun upgradeHttp2Success() = upgradeHttp2Result.receive()

    private suspend fun notifyException(context: RoutingContext?, exception: Throwable) {
        try {
            upgradeHttp2Result.offer(false)
            log.error(exception) { "HTTP1 server parser exception. id: ${connection.id}" }
            connectionListener.onException(context, exception).await()
        } catch (e: Exception) {
            log.error(e) { "HTTP1 server on exception. id: ${connection.id}" }
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
object EndHandleRequest : ParserMessage()