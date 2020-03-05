package com.fireflysource.net.http.server.impl

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.discard
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.common.v2.frame.DataFrame
import com.fireflysource.net.http.common.v2.frame.Frame
import com.fireflysource.net.http.common.v2.frame.HeadersFrame
import com.fireflysource.net.http.common.v2.stream.Stream
import com.fireflysource.net.http.server.HttpServerOutputChannel
import com.fireflysource.net.tcp.buffer.DelegatedOutputBufferArray
import com.fireflysource.net.tcp.buffer.OutputBufferArray
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Pengtao Qiu
 */
class Http2ServerOutputChannel(
    private val response: MetaData.Response,
    private val stream: Stream
) : HttpServerOutputChannel {

    companion object {
        private val log = SystemLogger.create(Http2ServerOutputChannel::class.java)
        private const val defaultMaxFrameSize = Frame.DEFAULT_MAX_LENGTH.toLong()
    }

    private val committed = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val messages = LinkedList<Http2OutputMessage>()

    override fun commit(): CompletableFuture<Void> {
        if (committed.compareAndSet(false, true)) {
            messages.offer(HeadersOutputMessage)
        }
        return Result.DONE
    }

    override fun isCommitted(): Boolean = committed.get()

    override fun write(byteBuffers: Array<ByteBuffer>, offset: Int, length: Int): CompletableFuture<Long> {
        val message = BuffersOutputMessage(byteBuffers, offset, length)
        messages.offer(message)
        writeOutputMessage()

        val future = CompletableFuture<Long>()
        future.complete(message.remaining())
        return future
    }

    override fun write(byteBufferList: List<ByteBuffer>, offset: Int, length: Int): CompletableFuture<Long> {
        return write(byteBufferList.toTypedArray(), offset, length)
    }

    override fun write(string: String): CompletableFuture<Int> {
        return write(string, StandardCharsets.UTF_8)
    }

    override fun write(string: String, charset: Charset): CompletableFuture<Int> {
        val byteBuffer = BufferUtils.toBuffer(string, charset)
        return write(byteBuffer)
    }

    override fun write(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        val message = BufferOutputMessage(byteBuffer)
        messages.offer(message)
        writeOutputMessage()

        val future = CompletableFuture<Int>()
        future.complete(byteBuffer.remaining())
        return future
    }

    override fun isOpen(): Boolean = closed.get()

    override fun closeFuture(): CompletableFuture<Void> {
        if (closed.compareAndSet(false, true)) {
            writeOutputMessage()
            val trailers = response.trailerSupplier?.get()
            if (trailers != null) {
                val trailerMetaData = MetaData.Response(trailers)
                trailerMetaData.isOnlyTrailer = true
                val headersFrameTrailer = HeadersFrame(stream.id, trailerMetaData, null, true)
                stream.headers(headersFrameTrailer, discard())
            }
        }
        return Result.DONE
    }

    override fun close() {
        closeFuture()
    }

    private fun writeOutputMessage() {
        val message = messages.poll()
        if (message != null) {
            val last = messages.isEmpty() && response.trailerSupplier == null
            when (message) {
                is HeadersOutputMessage -> writeHeaders(last)
                is BufferOutputMessage -> writeBuffer(message, last)
                is BuffersOutputMessage -> writeBuffers(message, last)
            }
        }
    }

    private fun writeHeaders(last: Boolean) {
        val headersFrame = HeadersFrame(stream.id, response, null, last)
        stream.headers(headersFrame, discard())
    }

    private fun writeBuffer(message: BufferOutputMessage, last: Boolean) {
        val dataFrame = DataFrame(stream.id, message.byteBuffer, last)
        stream.data(dataFrame, discard())
    }

    private fun writeBuffers(message: BuffersOutputMessage, last: Boolean) {
        val length = message.getCurrentLength()
        when {
            length == 1 -> {
                val byteBuffer = message.byteBuffers[message.getCurrentOffset()]
                val dataFrame = DataFrame(stream.id, byteBuffer, last)
                stream.data(dataFrame, discard())
            }
            length > 1 -> {
                while (message.hasRemaining()) {
                    val remaining = message.remaining()
                    val size = remaining.coerceAtMost(defaultMaxFrameSize).toInt()
                    val buffer = BufferUtils.allocate(size)
                    val pos = buffer.flipToFill()

                    log.debug { "HTTP2 outputs buffer array. before remaining: $remaining, size: $size" }
                    while (buffer.hasRemaining()) {
                        val offset = message.getCurrentOffset()
                        val src = message.byteBuffers[offset]
                        BufferUtils.put(src, buffer)
                        log.debug { "HTTP2 outputs buffer array. put offset: $offset" }
                    }

                    buffer.flipToFlush(pos)
                    val end = last && !message.hasRemaining()
                    val dataFrame = DataFrame(stream.id, buffer, end)
                    stream.data(dataFrame, discard())
                    log.debug { "HTTP2 outputs buffer array. after remaining: ${message.remaining()}, end: $end" }
                }
            }
        }
    }

}

sealed class Http2OutputMessage

object HeadersOutputMessage : Http2OutputMessage()

class BufferOutputMessage(val byteBuffer: ByteBuffer) : Http2OutputMessage()

class BuffersOutputMessage(
    val byteBuffers: Array<ByteBuffer>, val offset: Int, val length: Int,
    private val delegatedBufferArray: DelegatedOutputBufferArray = DelegatedOutputBufferArray(
        byteBuffers, offset, length, discard()
    )
) : OutputBufferArray by delegatedBufferArray, Http2OutputMessage()