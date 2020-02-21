package com.fireflysource.net.http.server.impl

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.server.HttpServerOutputChannel
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class Http1ServerOutputChannel(
    private val http1ServerConnection: Http1ServerConnection,
    private val response: MetaData.Response
) : HttpServerOutputChannel {

    private val committed = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    override fun isCommitted(): Boolean = committed.get()

    override fun commit(): CompletableFuture<Void> {
        return if (committed.compareAndSet(false, true)) {
            val header = Header(response, CompletableFuture())
            http1ServerConnection.sendResponseMessage(header)
            header.future
        } else {
            Result.DONE
        }
    }

    override fun write(byteBuffers: Array<ByteBuffer>, offset: Int, length: Int): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()
        val buffers = Http1OutputBuffers(byteBuffers, offset, length, Result.futureToConsumer(future))
        http1ServerConnection.sendResponseMessage(buffers)
        return future
    }

    override fun write(byteBufferList: List<ByteBuffer>, offset: Int, length: Int): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()
        val buffers = Http1OutputBufferList(byteBufferList, offset, length, Result.futureToConsumer(future))
        http1ServerConnection.sendResponseMessage(buffers)
        return future
    }

    override fun write(string: String): CompletableFuture<Int> {
        return write(string, StandardCharsets.UTF_8)
    }

    override fun write(string: String, charset: Charset): CompletableFuture<Int> {
        val buffer = BufferUtils.toBuffer(string, charset)
        return write(buffer)
    }

    override fun write(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        val future = CompletableFuture<Int>()
        val buffer = Http1OutputBuffer(byteBuffer, Result.futureToConsumer(future))
        http1ServerConnection.sendResponseMessage(buffer)
        return future
    }

    override fun closeFuture(): CompletableFuture<Void> {
        return if (closed.compareAndSet(false, true)) {
            val message = EndResponse(CompletableFuture())
            http1ServerConnection.sendResponseMessage(message)
            message.future
        } else {
            Result.DONE
        }
    }

    override fun isOpen(): Boolean = !closed.get()

    override fun close() {
        closeFuture()
    }
}