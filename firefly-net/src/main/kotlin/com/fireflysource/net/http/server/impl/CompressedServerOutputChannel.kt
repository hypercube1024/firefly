package com.fireflysource.net.http.server.impl

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.net.http.common.codec.ContentEncoded
import com.fireflysource.net.http.common.model.ContentEncoding
import com.fireflysource.net.http.server.HttpServerOutputChannel
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

class CompressedServerOutputChannel(
    private val outputChannel: HttpServerOutputChannel,
    private val contentEncoding: ContentEncoding,
    private val bufferSize: Int = 512
) : HttpServerOutputChannel {

    private var compressedOutputStream: OutputStream? = null
    private var outputStreamAdapter: ServerOutputStreamAdapter? = null

    private inner class ServerOutputStreamAdapter : OutputStream() {

        override fun write(b: Int) {
            val buffer = BufferUtils.allocate(1)
            val pos = buffer.flipToFill()
            buffer.put(b.toByte())
            buffer.flipToFlush(pos)
            outputChannel.write(buffer)
        }

        override fun write(b: ByteArray) {
            outputChannel.write(ByteBuffer.wrap(b))
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            outputChannel.write(ByteBuffer.wrap(b, off, len))
        }

    }

    override fun isCommitted(): Boolean = outputChannel.isCommitted

    override fun commit(): CompletableFuture<Void> {
        return outputChannel.commit().thenAccept {
            val adapter = ServerOutputStreamAdapter()
            compressedOutputStream = ContentEncoded.createEncodingOutputStream(adapter, contentEncoding, bufferSize)
        }
    }

    override fun write(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        val bytes = BufferUtils.toArray(byteBuffer)
        getOutput().write(bytes)
        val future = CompletableFuture<Int>()
        future.complete(bytes.size)
        return future
    }

    override fun write(byteBuffers: Array<ByteBuffer>, offset: Int, length: Int): CompletableFuture<Long> {
        val last = offset + length - 1
        val buffer = BufferUtils.merge((offset..last).map { byteBuffers[it].duplicate() })
        val bytes = BufferUtils.toArray(buffer)
        getOutput().write(bytes)
        val future = CompletableFuture<Long>()
        future.complete(bytes.size.toLong())
        return future
    }

    override fun write(byteBufferList: List<ByteBuffer>, offset: Int, length: Int): CompletableFuture<Long> {
        return write(byteBufferList.toTypedArray(), offset, length)
    }

    override fun write(string: String): CompletableFuture<Int> {
        return write(string, StandardCharsets.UTF_8)
    }

    override fun write(string: String, charset: Charset): CompletableFuture<Int> {
        val buffer = BufferUtils.toBuffer(string, charset)
        return write(buffer)
    }

    private fun getOutput(): OutputStream {
        val output = compressedOutputStream
        requireNotNull(output) { "The compressed output stream not create" }
        return output
    }

    override fun closeFuture(): CompletableFuture<Void> {
        compressedOutputStream?.close()
        outputStreamAdapter?.close()
        return outputChannel.closeFuture()
    }

    override fun close() {
        closeFuture()
    }

    override fun isOpen(): Boolean = outputChannel.isOpen
}