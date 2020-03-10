package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.common.coroutine.event
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.string.QuotedStringTokenizer
import com.fireflysource.common.string.QuotedStringTokenizer.unquote
import com.fireflysource.common.string.QuotedStringTokenizer.unquoteOnly
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.codec.MultiPartParser
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.HttpServerContentHandler
import com.fireflysource.net.http.server.MultiPart
import com.fireflysource.net.http.server.RoutingContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

class MultiPartContentHandler(
    private val maxFileSize: Long = 0,
    private val maxRequestSize: Long = 0,
    private val fileSizeThreshold: Int = 0,
    private val path: Path = tempPath
) : HttpServerContentHandler {

    companion object {
        private val tempPath = Paths.get(System.getProperty("java.io.tmpdir"))
    }

    init {
        require(maxRequestSize >= maxFileSize) { "The max request size must be greater than the max file size." }
        require(maxFileSize >= fileSizeThreshold) { "The max file size must be greater than the file size threshold." }
    }

    private val multiParts: List<AsyncMultiPart> = mutableListOf()
    private val multiPartChannel: Channel<MultiPartHandlerMessage> = Channel(Channel.UNLIMITED)
    private var firstMessage = true
    private var requestSize: Long = 0
    private var parser: MultiPartParser? = null
    private var byteBuffer: ByteBuffer = BufferUtils.EMPTY_BUFFER
    private val multiPartHandler = MultiPartHandler()
    private val job = parsingJob()
    private var part: AsyncMultiPart? = null

    private inner class MultiPartHandler : MultiPartParser.Handler {

        override fun startPart() {
            multiPartChannel.offer(StartPart)
        }

        override fun parsedField(name: String, value: String) {
            multiPartChannel.offer(PartField(name, value))
        }

        override fun headerComplete(): Boolean {
            multiPartChannel.offer(PartHeaderComplete)
            return false
        }

        override fun content(item: ByteBuffer, last: Boolean): Boolean {
            multiPartChannel.offer(PartContent(item, last))
            return false
        }

        override fun messageComplete(): Boolean {
            multiPartChannel.offer(PartMessageComplete)
            return true
        }

        override fun earlyEOF() {
            multiPartChannel.offer(PartEarlyEOF)
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
                is EndMultiPartHandler -> parser?.parse(byteBuffer, true)
                is StartPart -> part = AsyncMultiPart(maxFileSize, fileSizeThreshold, path)
                is PartField -> part?.httpFields?.add(message.name, message.value)
                is PartHeaderComplete -> {
                    val contentDisposition = part?.httpFields?.get("Content-Disposition")
                    requireNotNull(contentDisposition) { "Missing Content-Disposition header" }

                    val token = QuotedStringTokenizer(contentDisposition, ";", false, true)
                    var formData = false
                    var name: String? = null
                    var fileName: String? = null
                    while (token.hasMoreTokens()) {
                        val tokenValue = token.nextToken().trim().toLowerCase()
                        when {
                            tokenValue.startsWith("form-data") -> formData = true
                            tokenValue.startsWith("name=") -> name = value(tokenValue)
                            tokenValue.startsWith("filename=") -> fileName = fileNameValue(tokenValue)
                        }
                    }

                    require(formData) { "Part not form-data" }
                    requireNotNull(name) { "No name in part" }
                    part?.name = name
                    part?.fileName = fileName ?: ""
                }
                is PartContent -> part?.accept(message.byteBuffer, message.last)
                is PartMessageComplete -> {
                    part?.closeFileHandler()
                    break@parseLoop
                }
                is PartEarlyEOF -> {
                    part?.closeFileHandler()
                    throw BadMessageException(HttpStatus.BAD_REQUEST_400)
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
            contentTypeBoundary = unquote(value(contentType.substring(start, end)).trim())
        }
        return contentTypeBoundary
    }

    private fun value(headerLine: String): String {
        val idx = headerLine.indexOf('=')
        val value = headerLine.substring(idx + 1).trim()
        return unquoteOnly(value)
    }

    private fun fileNameValue(headerLine: String): String {
        val idx = headerLine.indexOf('=')
        var value = headerLine.substring(idx + 1).trim()

        return if (value.matches(".??[a-z,A-Z]\\:\\\\[^\\\\].*".toRegex())) {
            // incorrectly escaped IE filenames that have the whole path
            // we just strip any leading & trailing quotes and leave it as is
            val first = value[0]
            if (first == '"' || first == '\'') value = value.substring(1)
            val last = value[value.length - 1]
            if (last == '"' || last == '\'') value = value.substring(0, value.length - 1)
            value
        } else unquoteOnly(value, true)
        // unquote the string, but allow any backslashes that don't
        // form a valid escape sequence to remain as many browsers
        // even on *nix systems will not escape a filename containing
        // backslashes
    }

    override fun accept(byteBuffer: ByteBuffer, ctx: RoutingContext) {
        requestSize += byteBuffer.remaining()
        if (requestSize > maxRequestSize) {
            throw BadMessageException(HttpStatus.PAYLOAD_TOO_LARGE_413)
        }
        if (firstMessage) {
            multiPartChannel.offer(ParseMultiPartBoundary(ctx.httpFields[HttpHeader.CONTENT_TYPE]))
            firstMessage = false
        } else {
            multiPartChannel.offer(ParseMultiPartContent(byteBuffer))
        }
    }

    override fun closeFuture(): CompletableFuture<Void> =
        event { closeAwait() }.asCompletableFuture().thenCompose { Result.DONE }

    private suspend fun closeAwait() {
        multiPartChannel.offer(EndMultiPartHandler)
        job.join()
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
object StartPart : MultiPartHandlerMessage()
data class PartField(val name: String, val value: String) : MultiPartHandlerMessage()
object PartHeaderComplete : MultiPartHandlerMessage()
class PartContent(val byteBuffer: ByteBuffer, val last: Boolean) : MultiPartHandlerMessage()
object PartMessageComplete : MultiPartHandlerMessage()
object PartEarlyEOF : MultiPartHandlerMessage()
object EndMultiPartHandler : MultiPartHandlerMessage()