package com.fireflysource.net.http.common.content.handler

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.HttpStatus
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletableFuture

abstract class AbstractByteBufferContentHandler<T>(
    private val maxRequestBodySize: Long = 200 * 1024 * 1024
) : HttpContentHandler<T> {

    private val byteBufferList = LinkedList<ByteBuffer>()
    private var requestSize: Long = 0
    private val utf8String: String by lazy { toString(StandardCharsets.UTF_8) }

    override fun accept(byteBuffer: ByteBuffer, u: T) {
        requestSize += byteBuffer.remaining()
        if (requestSize > maxRequestBodySize) {
            throw BadMessageException(HttpStatus.PAYLOAD_TOO_LARGE_413)
        }

        byteBufferList.add(byteBuffer)
    }

    override fun closeFuture(): CompletableFuture<Void> = Result.DONE

    override fun close() {
    }

    fun getByteBuffers(): List<ByteBuffer> = byteBufferList

    fun toString(charset: Charset): String {
        if (byteBufferList.isEmpty()) return ""

        val size = byteBufferList.sumBy { it.remaining() }
        if (size <= 0) {
            return ""
        }

        val buffer = BufferUtils.allocate(size)
        val pos = buffer.flipToFill()
        byteBufferList.forEach {
            buffer.put(it)
            it.flip()
        }
        buffer.flipToFlush(pos)
        return BufferUtils.toString(buffer, charset)
    }

    override fun toString(): String = utf8String
}