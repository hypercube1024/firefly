package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.common.concurrent.CompletableFutures
import com.fireflysource.common.coroutine.event
import com.fireflysource.common.coroutine.pollAll
import com.fireflysource.common.string.QuotedStringTokenizer
import com.fireflysource.common.string.QuotedStringTokenizer.unquote
import com.fireflysource.common.string.QuotedStringTokenizer.unquoteOnly
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.codec.MultiPartParser
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.HttpServerContentHandler
import com.fireflysource.net.http.server.MultiPart
import com.fireflysource.net.http.server.RoutingContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CompletableFuture

class MultiPartContentHandler(
    private val maxUploadFileSize: Long = 200 * 1024 * 1024,
    private val maxRequestBodySize: Long = 200 * 1024 * 1024,
    private val uploadFileSizeThreshold: Int = 4 * 1024 * 1024,
    private val path: Path = tempPath
) : HttpServerContentHandler {

    companion object {
        private val log = SystemLogger.create(MultiPartContentHandler::class.java)
        private val tempPath = Paths.get(System.getProperty("java.io.tmpdir"))
    }

    init {
        require(maxRequestBodySize >= maxUploadFileSize) { "The max request size must be greater than the max file size." }
        require(maxUploadFileSize >= uploadFileSizeThreshold) { "The max file size must be greater than the file size threshold." }
    }

    private val multiParts: LinkedList<AsyncMultiPart> = LinkedList()
    private val multiPartChannel: Channel<MultiPartHandlerMessage> = Channel(Channel.UNLIMITED)
    private var firstMessage = true
    private var requestSize: Long = 0
    private var parser: MultiPartParser? = null
    private var byteBuffers: LinkedList<ByteBuffer> = LinkedList()
    private val multiPartHandler = MultiPartHandler()
    private var job: Job? = null
    private var part: AsyncMultiPart? = null
    private var parsingException: Throwable? = null

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
            try {
                when (val message = multiPartChannel.receive()) {
                    is ParseMultiPartBoundary -> parseBoundaryAndCreateMultiPartParser(message)
                    is ParseMultiPartContent -> parseContent(message)
                    is EndMultiPartHandler -> endMultiPartHandler()
                    is StartPart -> createMultiPart()
                    is PartField -> addMultiPartField(message)
                    is PartHeaderComplete -> handleHeaderComplete()
                    is PartContent -> acceptContent(message)
                    is PartMessageComplete -> {
                        handleMessageComplete()
                        break@parseLoop
                    }
                    is PartEarlyEOF -> handleEarlyEOF()
                }
            } catch (e: Throwable) {
                this@MultiPartContentHandler.parsingException = e
                break@parseLoop
            }
        }

        multiPartChannel.pollAll { }
        try {
            closeAllFileHandlers()
        } catch (e: Exception) {
            log.error(e) { "close file handlers exception" }
        }
    }


    private fun createMultiPart() {
        val part =
            AsyncMultiPart(
                maxUploadFileSize,
                uploadFileSizeThreshold,
                Paths.get(path.toString(), UUID.randomUUID().toString())
            )
        multiParts.add(part)
        this.part = part
        log.debug { "Create multi-part. $part" }
    }

    private fun addMultiPartField(message: PartField) {
        part?.httpFields?.add(message.name, message.value)
    }

    private fun handleHeaderComplete() {
        val contentDisposition = part?.httpFields?.get("Content-Disposition")
        requireNotNull(contentDisposition) { "Missing Content-Disposition header" }

        val token = QuotedStringTokenizer(contentDisposition, ";", false, true)
        var formData = false
        var name: String? = null
        var fileName: String? = null
        while (token.hasMoreTokens()) {
            val tokenValue = token.nextToken().trim()
            val lowerCaseValue = tokenValue.toLowerCase()
            when {
                lowerCaseValue.startsWith("form-data") -> formData = true
                lowerCaseValue.startsWith("name=") -> name = value(tokenValue)
                lowerCaseValue.startsWith("filename=") -> fileName = fileNameValue(tokenValue)
            }
        }

        require(formData) { "Part not form-data" }
        requireNotNull(name) { "No name in part" }
        part?.name = name
        part?.fileName = fileName ?: ""
        log.debug { "Multi-part header complete. name: $name, fileName: $fileName, fields: ${part?.httpFields?.size()}" }
    }

    private fun acceptContent(message: PartContent) {
        log.debug { "Accept multi-part content. size: ${message.byteBuffer.remaining()}, last: ${message.last}" }
        part?.accept(message.byteBuffer, message.last)
    }

    private suspend fun handleMessageComplete() {
        closeAllFileHandlers()
        log.debug { "Multi-part complete. part: $part" }
    }

    private suspend fun handleEarlyEOF() {
        closeAllFileHandlers()
        throw BadMessageException(HttpStatus.BAD_REQUEST_400)
    }

    private suspend fun closeAllFileHandlers() {
        var ex: Exception? = null
        multiParts.forEach {
            try {
                it.closeFileHandler()
            } catch (e: Exception) {
                ex = e
            }
        }
        val e = ex
        if (e != null) {
            throw e
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

    private fun parseBoundaryAndCreateMultiPartParser(message: ParseMultiPartBoundary) {
        val boundary = parseBoundary(message.contentType)
        log.debug { "Parsed multi-part boundary: $boundary" }
        if (boundary.isNotBlank()) {
            parser = MultiPartParser(multiPartHandler, boundary)
        } else {
            throw BadMessageException(HttpStatus.BAD_REQUEST_400)
        }
    }

    private fun parseContent(message: ParseMultiPartContent) {
        byteBuffers.offer(message.byteBuffer)
        log.debug { "Parse multi part content. state: ${parser?.state}, size: ${message.byteBuffer.remaining()}" }
        if (byteBuffers.size > 1) {
            parser?.parse(byteBuffers.poll(), false)
        }
    }

    private fun endMultiPartHandler() {
        val buffer = byteBuffers.poll()
        requireNotNull(buffer)
        log.debug { "End multi-part handler. buffers: ${byteBuffers.size}, size: ${buffer.remaining()}" }
        parser?.parse(buffer, true)
    }

    private fun value(headerLine: String): String {
        val idx = headerLine.indexOf('=')
        val value = headerLine.substring(idx + 1).trim()
        return unquoteOnly(value)
    }

    private fun fileNameValue(headerLine: String): String {
        val idx = headerLine.indexOf('=')
        var value = headerLine.substring(idx + 1).trim()

        return if (value.matches(".??[a-z,A-Z]:\\\\[^\\\\].*".toRegex())) {
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
        if (requestSize > maxRequestBodySize) {
            throw BadMessageException(HttpStatus.PAYLOAD_TOO_LARGE_413)
        }

        if (firstMessage) {
            job = parsingJob()
            multiPartChannel.offer(ParseMultiPartBoundary(ctx.httpFields[HttpHeader.CONTENT_TYPE]))
            firstMessage = false
        }
        multiPartChannel.offer(ParseMultiPartContent(byteBuffer))
    }

    override fun closeFuture(): CompletableFuture<Void> {
        val parsingJob = job
        return if (parsingJob != null) {
            if (parsingJob.isCompleted) complete()
            else event { closeAwait() }.asCompletableFuture().thenCompose { complete() }
        } else Result.DONE
    }

    private fun complete(): CompletableFuture<Void> {
        val e = parsingException
        return if (e != null) CompletableFutures.completeExceptionally(e) else Result.DONE
    }

    private suspend fun closeAwait() {
        multiPartChannel.offer(EndMultiPartHandler)
        job?.join()
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