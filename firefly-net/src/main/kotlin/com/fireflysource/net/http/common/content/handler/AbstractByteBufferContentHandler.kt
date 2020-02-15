package com.fireflysource.net.http.common.content.handler

import com.fireflysource.common.io.AsyncCloseable
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.sys.Result
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer

abstract class AbstractByteBufferContentHandler<T> : AsyncCloseable, BiConsumer<ByteBuffer, T> {

    private val byteBufferList = LinkedList<ByteBuffer>()
    private val utf8String: String by lazy { toString(StandardCharsets.UTF_8) }

    override fun accept(byteBuffer: ByteBuffer, u: T) {
        byteBufferList.add(byteBuffer)
    }

    override fun closeFuture(): CompletableFuture<Void> = Result.DONE

    override fun close() {
    }

    fun getByteBuffers(): List<ByteBuffer> = byteBufferList

    fun toString(charset: Charset): String {
        val size = byteBufferList.map { it.remaining() }.sum()
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