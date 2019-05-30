package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.exception.UnsupportedOperationException
import com.fireflysource.net.http.client.HttpClientContentProvider
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpHeader
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletableFuture


class MultiPartContentProvider : HttpClientContentProvider {

    private val contentType: String
    private val firstBoundary: ByteArray
    private val middleBoundary: ByteArray
    private val onlyBoundary: ByteArray
    private val lastBoundary: ByteArray
    private val parts: MutableList<Part> = LinkedList()

    private var index = 0
    private var state = State.FIRST_BOUNDARY
    private var open = true

    companion object {
        private const val CR_LF = "\r\n"
        private val COLON_SPACE_BYTES: ByteArray = byteArrayOf(':'.toByte(), ' '.toByte())
        private val CR_LF_BYTES: ByteArray = byteArrayOf('\r'.toByte(), '\n'.toByte())
    }

    init {
        val boundary = makeBoundary()
        this.contentType = "multipart/form-data; boundary=$boundary"

        val firstBoundaryLine = "--$boundary$CR_LF"
        this.firstBoundary = firstBoundaryLine.toByteArray(StandardCharsets.US_ASCII)

        val middleBoundaryLine = CR_LF + firstBoundaryLine
        this.middleBoundary = middleBoundaryLine.toByteArray(StandardCharsets.US_ASCII)

        val onlyBoundaryLine = "--$boundary--$CR_LF"
        this.onlyBoundary = onlyBoundaryLine.toByteArray(StandardCharsets.US_ASCII)

        val lastBoundaryLine = CR_LF + onlyBoundaryLine
        this.lastBoundary = lastBoundaryLine.toByteArray(StandardCharsets.US_ASCII)
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
        throw UnsupportedOperationException("The multi part content does not support the toByteBuffer method")
    }

    override fun close() {
        open = false
        state = State.COMPLETE
    }

    override fun read(byteBuffer: ByteBuffer): CompletableFuture<Int> = asyncGlobally {
        generate(byteBuffer)
    }.asCompletableFuture()

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
            builder.append(java.lang.Long.toString(if (rnd < 0) -rnd else rnd, 36))
        }
        builder.setLength(length + 16)
        return builder.toString()
    }

    private class Part(
        val name: String,
        val fileName: String?,
        val content: HttpClientContentProvider,
        val fields: HttpFields?,
        val contentType: String
    ) {
        val headers: ByteArray
        val length: Long

        init {
            // Compute the Content-Disposition.
            var contentDisposition = "Content-Disposition: form-data; name=\"$name\""
            if (fileName != null) {
                contentDisposition += "; filename=\"$fileName\""
            }
            contentDisposition += CR_LF

            // Compute the Content-Type.
            var contentType = fields?.get(HttpHeader.CONTENT_TYPE)
            if (contentType == null) {
                contentType = this.contentType
            }
            contentType = "Content-Type: $contentType$CR_LF"

            // Compute the headers
            if (fields == null || fields.size() == 0) {
                var headers = contentDisposition
                headers += contentType
                headers += CR_LF
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
                    buffer.write(COLON_SPACE_BYTES)
                    val value = field.value
                    if (value != null) {
                        buffer.write(value.toByteArray(StandardCharsets.UTF_8))
                    }
                    buffer.write(CR_LF_BYTES)
                }
                buffer.write(CR_LF_BYTES)
                headers = buffer.toByteArray()
            }

            length = if (content.length() >= 0) headers.size + content.length() else -1
        }

    }

    private enum class State {
        FIRST_BOUNDARY, HEADERS, CONTENT, MIDDLE_BOUNDARY, LAST_BOUNDARY, COMPLETE
    }
}