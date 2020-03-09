package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.common.coroutine.event
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.string.QuotedStringTokenizer.unquote
import com.fireflysource.common.string.QuotedStringTokenizer.unquoteOnly
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.codec.MultiPartParser
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.server.HttpServerContentHandler
import com.fireflysource.net.http.server.MultiPart
import com.fireflysource.net.http.server.RoutingContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

class MultiPartContentHandler(
    private val maxFileSize: Long = 0,
    private val maxRequestSize: Long = 0,
    private val fileSizeThreshold: Int = 0
) : HttpServerContentHandler {

    private val multiParts: List<AsyncMultiPart> = mutableListOf()
    private val multiPartChannel: Channel<MultiPartHandlerMessage> = Channel(Channel.UNLIMITED)
    private var firstMessage = true
    private var parser: MultiPartParser? = null
    private var byteBuffer: ByteBuffer = BufferUtils.EMPTY_BUFFER
    private val multiPartHandler = MultiPartHandler()
    private val job = parsingJob()

    private inner class MultiPartHandler : MultiPartParser.Handler {

        private val httpFields = HttpFields()

        override fun startPart() {

        }

        override fun parsedField(name: String, value: String) {
            httpFields.add(name, value)
        }

        override fun headerComplete(): Boolean {
            return false
        }

        override fun content(item: ByteBuffer, last: Boolean): Boolean {
            return false
        }

        override fun messageComplete(): Boolean {
            return true
        }

        override fun earlyEOF() {

        }
    }

    private fun parsingJob() = event {
        parseLoop@ while (true) {
            when (val message = multiPartChannel.receive()) {
                is ParseMultiPartBoundary -> {
                    val boundary = parseBoundary(message.contentType)
                    if (boundary.isNotBlank()) {
                        parser = MultiPartParser(multiPartHandler, boundary)
                    } else {
                        break@parseLoop
                    }
                }
                is ParseMultiPartContent -> {
                    byteBuffer = message.byteBuffer
                    parser?.parse(message.byteBuffer, false)
                }
                is EndMultiPartHandler -> {
                    parser?.parse(byteBuffer, true)
                    break@parseLoop
                }
            }
        }
    }

    private fun parseBoundary(contentType: String?): String {
        if (contentType == null) return ""
        if (!contentType.startsWith("multipart/form-data")) return ""

        var contentTypeBoundary = ""
        val start: Int = contentType.indexOf("boundary=")
        if (start >= 0) {
            var end: Int = contentType.indexOf(";", start)
            end = if (end < 0) contentType.length else end
            contentTypeBoundary = unquote(value(contentType.substring(start, end)).trim { it <= ' ' })
        }
        return contentTypeBoundary
    }

    private fun value(nameEqualsValue: String): String {
        val idx = nameEqualsValue.indexOf('=')
        val value = nameEqualsValue.substring(idx + 1).trim { it <= ' ' }
        return unquoteOnly(value)
    }

    override fun accept(byteBuffer: ByteBuffer, ctx: RoutingContext) {
        if (firstMessage) {
            multiPartChannel.offer(ParseMultiPartBoundary(ctx.httpFields[HttpHeader.CONTENT_TYPE]))
            firstMessage = false
        } else {
            multiPartChannel.offer(ParseMultiPartContent(byteBuffer))
        }
    }

    override fun closeFuture(): CompletableFuture<Void> {
        multiPartChannel.offer(EndMultiPartHandler)
        return job.asCompletableFuture().thenCompose { Result.DONE }
    }

    override fun close() {
        closeFuture()
    }

    fun getPart(name: String): MultiPart? {
        return multiParts.find { it.name == name }
    }

    fun getParts(): List<MultiPart> = multiParts
}

sealed class MultiPartHandlerMessage
class ParseMultiPartBoundary(val contentType: String?) : MultiPartHandlerMessage()
class ParseMultiPartContent(val byteBuffer: ByteBuffer) : MultiPartHandlerMessage()
object EndMultiPartHandler : MultiPartHandlerMessage()