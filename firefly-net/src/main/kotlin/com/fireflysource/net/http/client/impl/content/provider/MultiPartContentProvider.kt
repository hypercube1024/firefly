package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.common.coroutine.CoroutineDispatchers.singleThread
import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.exception.UnsupportedOperationException
import com.fireflysource.common.io.closeAsync
import com.fireflysource.net.http.client.HttpClientContentProvider
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpHeader
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletableFuture


class MultiPartContentProvider : HttpClientContentProvider {

    companion object {
        private const val newLine = "\r\n"
        private val colonSpaceBytes: ByteArray = byteArrayOf(':'.toByte(), ' '.toByte())
        private val newLineBytes: ByteArray = byteArrayOf('\r'.toByte(), '\n'.toByte())
    }

    val contentType: String
    private val firstBoundary: ByteArray
    private val middleBoundary: ByteArray
    private val onlyBoundary: ByteArray
    private val lastBoundary: ByteArray
    private val parts: MutableList<Part> = LinkedList()

    private var index = 0
    private var state = State.FIRST_BOUNDARY
    private var open = true

    private val readChannel: Channel<ReadMultiPartMessage> = Channel(Channel.UNLIMITED)
    private val readJob: Job

    init {
        val boundary = makeBoundary()
        this.contentType = "multipart/form-data; boundary=$boundary"

        val firstBoundaryLine = "--$boundary$newLine"
        this.firstBoundary = firstBoundaryLine.toByteArray(StandardCharsets.US_ASCII)

        val middleBoundaryLine = newLine + firstBoundaryLine
        this.middleBoundary = middleBoundaryLine.toByteArray(StandardCharsets.US_ASCII)

        val onlyBoundaryLine = "--$boundary--$newLine"
        this.onlyBoundary = onlyBoundaryLine.toByteArray(StandardCharsets.US_ASCII)

        val lastBoundaryLine = newLine + onlyBoundaryLine
        this.lastBoundary = lastBoundaryLine.toByteArray(StandardCharsets.US_ASCII)

        readJob = launchGlobally(singleThread) {
            readMessageLoop@ while (true) {
                when (val readMultiPartMessage = readChannel.receive()) {
                    is ReadMultiPartRequest -> {
                        val (buf, future) = readMultiPartMessage
                        try {
                            val len = generate(buf)
                            future.complete(len)
                        } catch (e: Exception) {
                            future.completeExceptionally(e)
                        }
                    }
                    is EndReadMultiPart -> {
                        open = false
                        state = State.COMPLETE
                        parts.forEach { p -> p.closeAsync() }
                        break@readMessageLoop
                    }
                }

            }
        }
    }

    /**
     * <p>Adds a field part with the given {@code name} as field name, and the given
     * {@code content} as part content.</p>
     *
     * @param name    the part name
     * @param content the part content
     * @param fields  the headers associated with this part
     */
    fun addFieldPart(name: String, content: HttpClientContentProvider, fields: HttpFields?) {
        parts.add(Part(name, null, content, fields, "text/plain"))
    }

    /**
     * <p>Adds a file part with the given {@code name} as field name, the given
     * {@code fileName} as file name, and the given {@code content} as part content.</p>
     *
     * @param name     the part name
     * @param fileName the file name associated to this part
     * @param content  the part content
     * @param fields   the headers associated with this part
     */
    fun addFilePart(name: String, fileName: String?, content: HttpClientContentProvider, fields: HttpFields?) {
        parts.add(Part(name, fileName, content, fields, "application/octet-stream"))
    }

    override fun length(): Long {
        // Compute the length, if possible.
        if (parts.isEmpty()) {
            return onlyBoundary.size.toLong()
        } else {
            var result: Long = 0
            for (i in 0 until parts.size) {
                result += if (i == 0) firstBoundary.size.toLong() else middleBoundary.size.toLong()
                val part = parts[i]
                val partLength = part.length
                result += partLength
                if (partLength < 0) {
                    result = -1
                    break
                }
            }
            if (result > 0) {
                result += lastBoundary.size.toLong()
            }
            return result
        }
    }

    override fun isOpen(): Boolean = open

    override fun toByteBuffer(): ByteBuffer {
        throw UnsupportedOperationException("The multi part content does not support this method")
    }

    override fun close() {
        readChannel.offer(EndReadMultiPart)
    }

    override fun read(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        if (!isOpen) {
            return endStream()
        }

        if (state == State.COMPLETE) {
            return endStream()
        }

        val future = CompletableFuture<Int>()
        readChannel.offer(ReadMultiPartRequest(byteBuffer, future))
        return future
    }

    private suspend fun generate(byteBuffer: ByteBuffer): Int {
        while (true) {
            when (state) {
                State.FIRST_BOUNDARY -> {
                    return if (parts.isEmpty()) {
                        state = State.COMPLETE
                        byteBuffer.put(onlyBoundary)
                        onlyBoundary.size
                    } else {
                        state = State.HEADERS
                        byteBuffer.put(firstBoundary)
                        firstBoundary.size
                    }
                }
                State.HEADERS -> {
                    val part = parts[index]
                    state = State.CONTENT
                    byteBuffer.put(part.headers)
                    return part.headers.size
                }
                State.CONTENT -> {
                    val part = parts[index]
                    val len = part.content.read(byteBuffer).await()
                    if (len >= 0) {
                        return len
                    } else {
                        ++index
                        state = if (index == parts.size) State.LAST_BOUNDARY else State.MIDDLE_BOUNDARY
                    }
                }
                State.MIDDLE_BOUNDARY -> {
                    state = State.HEADERS
                    byteBuffer.put(middleBoundary)
                    return middleBoundary.size
                }
                State.LAST_BOUNDARY -> {
                    state = State.COMPLETE
                    byteBuffer.put(lastBoundary)
                    return lastBoundary.size
                }
                State.COMPLETE -> {
                    open = false
                    return -1
                }
            }
        }
    }

    private fun makeBoundary(): String {
        val random = Random()
        val builder = StringBuilder("FireflyHttpClientBoundary")
        val length = builder.length
        while (builder.length < length + 16) {
            val rnd = random.nextLong()
            builder.append((if (rnd < 0) -rnd else rnd).toString(36))
        }
        builder.setLength(length + 16)
        return builder.toString()
    }

    private class Part(
        name: String,
        fileName: String?,
        val content: HttpClientContentProvider,
        fields: HttpFields?,
        val contentType: String
    ) : Closeable {
        val headers: ByteArray
        val length: Long

        init {
            // Compute the Content-Disposition.
            var contentDisposition = "Content-Disposition: form-data; name=\"$name\""
            if (fileName != null) {
                contentDisposition += "; filename=\"$fileName\""
            }
            contentDisposition += newLine

            // Compute the Content-Type.
            var contentType = fields?.get(HttpHeader.CONTENT_TYPE)
            if (contentType == null) {
                contentType = this.contentType
            }
            contentType = "Content-Type: $contentType$newLine"

            // Compute the headers
            if (fields == null || fields.size() == 0) {
                var headers = contentDisposition
                headers += contentType
                headers += newLine
                this.headers = headers.toByteArray(StandardCharsets.UTF_8)
            } else {
                val buffer = ByteArrayOutputStream((fields.size() + 1) * contentDisposition.length)
                buffer.write(contentDisposition.toByteArray(StandardCharsets.UTF_8))
                buffer.write(contentType.toByteArray(StandardCharsets.UTF_8))
                for (field in fields) {
                    if (HttpHeader.CONTENT_TYPE == field.header) {
                        continue
                    }
                    buffer.write(field.name.toByteArray(StandardCharsets.US_ASCII))
                    buffer.write(colonSpaceBytes)
                    val value = field.value
                    if (value != null) {
                        buffer.write(value.toByteArray(StandardCharsets.UTF_8))
                    }
                    buffer.write(newLineBytes)
                }
                buffer.write(newLineBytes)
                headers = buffer.toByteArray()
            }

            length = if (content.length() >= 0) headers.size + content.length() else -1
        }

        override fun close() {
            content.close()
        }

    }

    private enum class State {
        FIRST_BOUNDARY, HEADERS, CONTENT, MIDDLE_BOUNDARY, LAST_BOUNDARY, COMPLETE
    }
}

sealed class ReadMultiPartMessage
data class ReadMultiPartRequest(val byteBuffer: ByteBuffer, val future: CompletableFuture<Int>) : ReadMultiPartMessage()
object EndReadMultiPart : ReadMultiPartMessage()