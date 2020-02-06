package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.net.http.client.HttpClientContentProvider
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import kotlin.math.min

open class ByteBufferContentProvider(private val content: ByteBuffer) : HttpClientContentProvider {

    private val buffer = content.duplicate()
    private val length = content.remaining().toLong()
    private var open = true

    override fun isOpen(): Boolean = open

    override fun close() {
        open = false
    }

    override fun length(): Long = length

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

    override fun toByteBuffer(): ByteBuffer = ByteBuffer.wrap(BufferUtils.toArray(buffer))

    override fun closeFuture(): CompletableFuture<Void> {
        close()
        val future = CompletableFuture<Void>()
        future.complete(null)
        return future
    }
}