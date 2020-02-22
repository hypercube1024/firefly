package com.fireflysource.net.http.common.content.provider

import com.fireflysource.common.io.InputChannel
import com.fireflysource.common.sys.Result
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import kotlin.math.min

abstract class AbstractByteBufferContentProvider(private val content: ByteBuffer) : InputChannel {
    private val buffer = content.duplicate()
    private val length = content.remaining().toLong()
    private var open = true

    override fun isOpen(): Boolean = open

    override fun close() {
        open = false
    }

    open fun length(): Long = length

    open fun toByteBuffer(): ByteBuffer = buffer.duplicate()

    override fun read(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        if (!isOpen) {
            return endStream()
        }

        if (!content.hasRemaining()) {
            return endStream()
        }

        if (!byteBuffer.hasRemaining()) {
            val future = CompletableFuture<Int>()
            future.complete(0)
            return future
        }

        val len = min(content.remaining(), byteBuffer.remaining())
        val to = ByteArray(len)
        content.get(to)
        byteBuffer.put(to)

        val future = CompletableFuture<Int>()
        future.complete(len)
        return future
    }

    override fun closeFuture(): CompletableFuture<Void> {
        close()
        return Result.DONE
    }
}