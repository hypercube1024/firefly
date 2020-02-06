package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.net.http.client.HttpClientContentProvider
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture

class StringContentProvider(val content: String, val charset: Charset) : HttpClientContentProvider {

    private var open = true
    private var index = 0

    override fun isOpen(): Boolean = open

    override fun close() {
        open = false
    }

    override fun length(): Long = content.length.toLong()

    override fun read(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        if (!isOpen) {
            return endStream()
        }

        val len = length().toInt()
        if (index == len) {
            return endStream()
        }

        if (!byteBuffer.hasRemaining()) {
            val future = CompletableFuture<Int>()
            future.complete(0)
            return future
        }

        val endIndex = (index + byteBuffer.remaining()).coerceAtMost(len)
        val bytes = content.substring(index, endIndex).toByteArray(charset)
        byteBuffer.put(bytes)
        index += bytes.size

        val future = CompletableFuture<Int>()
        future.complete(bytes.size)
        return future
    }

    override fun toByteBuffer(): ByteBuffer {
        return ByteBuffer.wrap(content.toByteArray(charset))
    }

    override fun closeFuture(): CompletableFuture<Void> {
        close()
        val future = CompletableFuture<Void>()
        future.complete(null)
        return future
    }
}