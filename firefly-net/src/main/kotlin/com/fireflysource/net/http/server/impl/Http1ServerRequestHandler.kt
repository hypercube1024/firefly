package com.fireflysource.net.http.server.impl

import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.server.HttpServerConnection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.net.URL
import java.nio.ByteBuffer

class Http1ServerRequestHandler(val connection: Http1ServerConnection) : HttpParser.RequestHandler {

    companion object {
        private val log = SystemLogger.create(Http1ServerRequestHandler::class.java)
    }

    var connectionListener: HttpServerConnection.Listener? = null
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
                connectionListener?.onException(context, e)?.await()
            }
        }
    }

    private fun newRoutingContext() {
        val httpServerRequest = AsyncHttpServerRequest(MetaData.Request(request))
        context = AsyncRoutingContext(httpServerRequest, AsyncHttpServerResponse(connection), connection)
    }

    private fun reset() {
        request.recycle()
        context = null
    }

    private suspend fun onParserMessage(message: ParserMessage) {
        when (message) {
            is HeaderComplete -> {
                newRoutingContext()
                connectionListener?.onHeaderComplete(context)?.await()
                reset()
            }
            is Content -> {
                context?.request?.contentHandler?.accept(message.byteBuffer, context)
            }
            is ContentComplete -> {
                context?.request?.contentHandler?.closeFuture()?.await()
            }
            is MessageComplete -> {
                context?.request?.isRequestComplete = true
                connectionListener?.onHttpRequestComplete(context)?.await()
                reset()
            }
            is BadMessage -> {
                log.error(message.exception) { "Receive the bad HTTP1 message. id: ${connection.id}" }
                connectionListener?.onException(context, message.exception)?.await()
                reset()
            }
            is EarlyEOF -> {
                connectionListener?.onException(context, IllegalStateException("Parser early EOF"))?.await()
                reset()
            }
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