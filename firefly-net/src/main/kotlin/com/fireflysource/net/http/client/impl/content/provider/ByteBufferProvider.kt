package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.net.http.client.HttpClientContentProvider
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

class ByteBufferProvider(val content: ByteBuffer) : HttpClientContentProvider {

    private val buffer = content.duplicate()
    private var open = true

    override fun isOpen(): Boolean = open

    override fun close() {
        open = false
    }

    override fun length(): Long = buffer.remaining().toLong()

    override fun read(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        if (!isOpen) {
            return endStream()
        }

        if (!byteBuffer.hasRemaining()) {
            return endStream()
        }

        val len = Math.min(content.remaining(), byteBuffer.remaining())
        val to = ByteArray(len)
        content.get(to)
        byteBuffer.put(to)

        val future = CompletableFuture<Int>()
        future.complete(len)
        return future
    }

    override fun toByteBuffer(): ByteBuffer = ByteBuffer.wrap(BufferUtils.toArray(buffer))
}