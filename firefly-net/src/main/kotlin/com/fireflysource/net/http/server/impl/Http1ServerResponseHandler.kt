package com.fireflysource.net.http.server.impl

import com.fireflysource.common.`object`.Assert
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator
import com.fireflysource.net.http.common.v1.encoder.assert
import com.fireflysource.net.tcp.buffer.DelegatedOutputBufferArray
import com.fireflysource.net.tcp.buffer.OutputBufferArray
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

class Http1ServerResponseHandler(private val http1ServerConnection: Http1ServerConnection) {

    companion object {
        private val log = SystemLogger.create(Http1ServerResponseHandler::class.java)
    }

    private val generator = HttpGenerator()
    private val headerBuffer: ByteBuffer by lazy { BufferUtils.allocateDirect(http1ServerConnection.getHeaderBufferSize()) }
    private val chunkBuffer: ByteBuffer by lazy { BufferUtils.allocateDirect(HttpGenerator.CHUNK_SIZE) }
    private val responseChannel: Channel<Http1ResponseMessage> = Channel(Channel.UNLIMITED)

    fun sendResponseMessage(message: Http1ResponseMessage) {
        responseChannel.offer(message)
    }

    fun generateResponseJob() = http1ServerConnection.coroutineScope.launch {
        while (true) {
            when (val message = responseChannel.receive()) {
                is Header -> generateHeader(message)
                is Http1OutputBuffer -> generateContent(message)
                is Http1OutputBuffers -> generateContent(message)
                is Http1OutputBufferList -> generateContent(message)
                is EndResponse -> completeContent(message)
            }
        }
    }

    private suspend fun generateHeader(header: Header) {
        val (response, future) = header
        try {
            generator.generateResponse(response, false, headerBuffer, null, null, false)
                .assertFlush()
            flushHeaderBuffer()
            Result.done(future)
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }
    }

    private suspend fun generateContent(http1OutputBuffers: Http1OutputBuffers) {
        try {
            val content = LinkedList<ByteBuffer>()
            val offset = http1OutputBuffers.getCurrentOffset()
            val lastIndex = http1OutputBuffers.getLastIndex()
            (offset..lastIndex).forEach {
                val buffer = http1OutputBuffers.buffers[it]
                if (generator.isChunking) {
                    val chunk = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE)
                    generator.generateResponse(null, false, null, chunk, buffer, false)
                        .assertFlush()
                    content.add(chunk)
                    content.add(buffer)
                } else {
                    generator.generateResponse(null, false, null, null, buffer, false)
                        .assertFlush()
                    content.add(buffer)
                }
            }
            val length = http1ServerConnection.tcpConnection.write(content, 0, content.size).await()
            http1OutputBuffers.result.accept(Result(true, length, null))
        } catch (e: Exception) {
            http1OutputBuffers.result.accept(Result(false, -1, e))
        }
    }

    private suspend fun generateContent(http1OutputBuffer: Http1OutputBuffer) {
        val (buffer, result) = http1OutputBuffer
        try {
            val length = if (generator.isChunking) {
                generator.generateResponse(null, false, null, chunkBuffer, buffer, false)
                    .assertFlush()
                flushChunkedContentBuffer(buffer).toInt()
            } else {
                generator.generateResponse(null, false, null, null, buffer, false)
                    .assertFlush()
                flushContentBuffer(buffer)
            }
            result.accept(Result(true, length, null))
        } catch (e: Exception) {
            result.accept(Result(false, -1, e))
        }
    }

    private fun HttpGenerator.Result.assertFlush() {
        this.assert(HttpGenerator.Result.FLUSH)
        assert(HttpGenerator.State.COMMITTED)
    }

    private suspend fun completeContent(endResponse: EndResponse) {
        try {
            completing()
            if (generator.isChunking) {
                when (val generateResult = generator.generateResponse(null, false, null, chunkBuffer, null, true)) {
                    HttpGenerator.Result.FLUSH -> {
                        assert(HttpGenerator.State.COMPLETING)
                        flushChunkBuffer()
                    }
                    HttpGenerator.Result.NEED_CHUNK_TRAILER -> generateTrailer()
                    else -> throw IllegalStateException("The HTTP server generator result error. $generateResult")
                }
            }
            end(endResponse)
            Result.done(endResponse.future)
        } catch (e: Exception) {
            endResponse.future.completeExceptionally(e)
        }
    }

    private fun completing() {
        generator.generateResponse(null, false, null, null, null, true)
            .assert(HttpGenerator.Result.CONTINUE)
        assert(HttpGenerator.State.COMPLETING)
    }

    private fun end(endResponse: EndResponse) {
        http1ServerConnection.tcpConnection.flush()
        val result = generator.generateResponse(null, false, null, null, null, true)
        if (result == HttpGenerator.Result.SHUTDOWN_OUT || endResponse.closeConnection) {
            http1ServerConnection.closeFuture()
            log.debug { "HTTP1 server connection is closing. id: ${http1ServerConnection.id}" }
        }

        assert(HttpGenerator.State.END)
        generator.reset()
    }

    private suspend fun generateTrailer() {
        generator.generateResponse(null, false, null, headerBuffer, null, true)
            .assert(HttpGenerator.Result.FLUSH)
        assert(HttpGenerator.State.COMPLETING)
        flushHeaderBuffer()
    }

    private fun assert(expectState: HttpGenerator.State) {
        Assert.state(
            generator.isState(expectState),
            "The HTTP generator state error. ${generator.state}"
        )
    }

    private suspend fun flushHeaderBuffer() {
        if (headerBuffer.hasRemaining()) {
            val size = http1ServerConnection.tcpConnection.write(headerBuffer).await()
            log.debug { "flush header bytes: $size" }
        }
        BufferUtils.clear(headerBuffer)
    }

    private suspend fun flushContentBuffer(contentBuffer: ByteBuffer): Int {
        return if (contentBuffer.hasRemaining()) {
            val size = http1ServerConnection.tcpConnection.write(contentBuffer).await()
            log.debug { "flush content bytes: $size" }
            size
        } else 0
    }

    private suspend fun flushChunkedContentBuffer(contentBuffer: ByteBuffer): Long {
        val bufArray = arrayOf(chunkBuffer, contentBuffer)
        val remaining = bufArray.map { it.remaining().toLong() }.sum()
        val length = if (remaining > 0) {
            val len = http1ServerConnection.tcpConnection.write(bufArray, 0, bufArray.size).await()
            log.debug { "flush chunked content bytes: $len" }
            len
        } else 0
        BufferUtils.clear(chunkBuffer)
        return length
    }

    private suspend fun flushChunkBuffer() {
        if (chunkBuffer.hasRemaining()) {
            val size = http1ServerConnection.tcpConnection.write(chunkBuffer).await()
            log.debug { "flush chunked bytes: $size" }
        }
        BufferUtils.clear(chunkBuffer)
    }

}

sealed class Http1ResponseMessage

data class Header(
    val response: MetaData.Response,
    val future: CompletableFuture<Void>
) : Http1ResponseMessage()

data class Http1OutputBuffer(
    val buffer: ByteBuffer, val result: Consumer<Result<Int>>
) : Http1ResponseMessage()

open class Http1OutputBuffers(
    val buffers: Array<ByteBuffer>,
    val offset: Int,
    val length: Int,
    val result: Consumer<Result<Long>>,
    private val outputBufferArray: DelegatedOutputBufferArray = DelegatedOutputBufferArray(
        buffers, offset, length, result
    )
) : OutputBufferArray by outputBufferArray, Http1ResponseMessage()

class Http1OutputBufferList(
    bufferList: List<ByteBuffer>,
    offset: Int,
    length: Int,
    result: Consumer<Result<Long>>
) : Http1OutputBuffers(bufferList.toTypedArray(), offset, length, result)

data class EndResponse(val future: CompletableFuture<Void>, val closeConnection: Boolean) : Http1ResponseMessage()