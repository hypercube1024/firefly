package com.fireflysource.net.http.server.impl

import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.common.v1.decoder.HttpParser
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
    private var request = MetaData.Request(HttpFields())
    private var context: AsyncRoutingContext? = null

    init {
        handleParserMessageJob()
    }

    private fun handleParserMessageJob() = connection.coroutineScope.launch {
        parserLoop@ while (true) {
            val message = parserChannel.receive()
            try {
                onParserMessage(message)
            } catch (e: Exception) {
                log.error(e) { "Handle HTTP1 server parser message exception." }
                notifyException(context, e)
            }
        }
    }

    private suspend fun onParserMessage(message: ParserMessage) {
        when (message) {
            is HeaderComplete -> {
                val httpServerRequest = AsyncHttpServerRequest(MetaData.Request(request))
                val ctx = AsyncRoutingContext(httpServerRequest, Http1ServerResponse(connection), connection)
                context = ctx
                request.recycle()
                notifyHeaderComplete(ctx)
            }
            is Content -> {
                try {
                    val ctx = context
                    requireNotNull(ctx)
                    ctx.request.contentHandler.accept(message.byteBuffer, context)
                    log.debug { "HTTP1 server receives content success. id: ${connection.id}" }
                } catch (e: Exception) {
                    log.error(e) { "HTTP1 server accepts content exception. id: ${connection.id}" }
                }
            }
            is ContentComplete -> {
                try {
                    val ctx = context
                    requireNotNull(ctx)
                    ctx.request.contentHandler.closeFuture().await()
                } catch (e: Exception) {
                    log.error(e) { "HTTP1 server completes content exception. id: ${connection.id}" }
                }
            }
            is MessageComplete -> {
                val ctx = context
                requireNotNull(ctx)
                ctx.request.isRequestComplete = true
                notifyHttpRequestComplete(ctx)
                reset()
            }
            is BadMessage -> {
                log.error(message.exception) { "Receive the bad HTTP1 message. id: ${connection.id}" }
                notifyException(context, message.exception)
                reset()
            }
            is EarlyEOF -> {
                log.error { "HTTP1 server parser early EOF. id: ${connection.id}" }
                notifyException(context, IllegalStateException("Parser early EOF"))
                reset()
            }
        }
    }

    private fun reset() {
        context = null
        request.recycle()
    }

    private suspend fun notifyHeaderComplete(context: RoutingContext) {
        try {
            connectionListener.onHeaderComplete(context).await()
            log.debug { "HTTP1 server handles header complete success. id: ${connection.id}" }
        } catch (e: Exception) {
            log.error(e) { "HTTP1 server handles header complete exception. id: ${connection.id}" }
        }
    }

    private suspend fun notifyHttpRequestComplete(context: RoutingContext) {
        try {
            connectionListener.onHttpRequestComplete(context).await()
            log.debug { "HTTP1 server handles request success. id: ${connection.id}" }
        } catch (e: Exception) {
            log.error(e) { "HTTP1 server handles request exception. id: ${connection.id}" }
        }
    }

    private suspend fun notifyException(context: RoutingContext?, exception: Exception) {
        try {
            connectionListener.onException(context, exception).await()
        } catch (e: Exception) {
            log.error(e) { "HTTP1 server receives the error. id: ${connection.id}" }
        }
    }

    override fun startRequest(method: String, uri: String, version: HttpVersion): Boolean {
        request.method = method
        request.uri = HttpURI(URL(uri).toURI())
        request.httpVersion = version
        return false
    }

    override fun getHeaderCacheSize(): Int = 4096

    override fun parsedHeader(field: HttpField) {
        request.fields.add(field)
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
object HeaderComplete : ParserMessage()
class Content(val byteBuffer: ByteBuffer) : ParserMessage()
object ContentComplete : ParserMessage()
object MessageComplete : ParserMessage()
object EarlyEOF : ParserMessage()
class BadMessage(val exception: Exception) : ParserMessage()