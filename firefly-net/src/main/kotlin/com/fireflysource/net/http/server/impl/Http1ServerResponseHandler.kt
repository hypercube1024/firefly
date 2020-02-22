package com.fireflysource.net.http.server.impl

import com.fireflysource.common.`object`.Assert
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator
import com.fireflysource.net.tcp.buffer.OutputBuffers
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

    // generator buffer
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
            val result = generator.generateResponse(response, false, headerBuffer, null, null, false)
            checkResult(result)
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
                    val generateResult = generator.generateResponse(null, false, null, chunk, buffer, false)
                    checkResult(generateResult)
                    content.add(chunk)
                    content.add(buffer)
                } else {
                    val generateResult = generator.generateResponse(null, false, null, null, buffer, false)
                    checkResult(generateResult)
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
                val generateResult = generator.generateResponse(null, false, null, chunkBuffer, buffer, false)
                checkResult(generateResult)
                flushChunkedContentBuffer(buffer).toInt()
            } else {
                val generateResult = generator.generateResponse(null, false, null, null, buffer, false)
                checkResult(generateResult)
                flushContentBuffer(buffer)
            }
            result.accept(Result(true, length, null))
        } catch (e: Exception) {
            result.accept(Result(false, -1, e))
        }
    }

    private fun checkResult(generateResult: HttpGenerator.Result) {
        Assert.state(
            generateResult == HttpGenerator.Result.FLUSH,
            "The HTTP server generator result error. $generateResult"
        )
        Assert.state(
            generator.isState(HttpGenerator.State.COMMITTED),
            "The HTTP server generator state error. ${generator.state}"
        )
    }

    private suspend fun completeContent(endResponse: EndResponse) {
        try {
            completing()
            if (generator.isChunking) {
                when (val generateResult = generator.generateResponse(null, false, null, chunkBuffer, null, true)) {
                    HttpGenerator.Result.FLUSH -> {
                        Assert.state(
                            generator.isState(HttpGenerator.State.COMPLETING),
                            "The HTTP server generator state error. ${generator.state}"
                        )
                        flushChunkBuffer()
                    }
                    HttpGenerator.Result.NEED_CHUNK_TRAILER -> {
                        generateTrailer()
                    }
                    else -> throw IllegalStateException("The HTTP server generator result error. $generateResult")
                }
            }
            end()
            Result.done(endResponse.future)
        } catch (e: Exception) {
            endResponse.future.completeExceptionally(e)
        }
    }

    private fun completing() {
        val generateResult = generator.generateResponse(null, false, null, null, null, true)
        Assert.state(
            generateResult == HttpGenerator.Result.CONTINUE,
            "The HTTP server generator result error. $generateResult"
        )
        Assert.state(
            generator.isState(HttpGenerator.State.COMPLETING),
            "The HTTP server generator state error. ${generator.state}"
        )
    }

    private fun end() {
        val generateResult = generator.generateResponse(null, false, null, null, null, true)
        Assert.state(
            generateResult == HttpGenerator.Result.DONE,
            "The HTTP server generator result error. $generateResult"
        )
        Assert.state(
            generator.isState(HttpGenerator.State.END),
            "The HTTP server generator state error. ${generator.state}"
        )
        generator.reset()
    }

    private suspend fun generateTrailer() {
        val generateResult = generator.generateResponse(null, false, null, headerBuffer, null, true)
        Assert.state(
            generateResult == HttpGenerator.Result.FLUSH,
            "The HTTP server generator result error. $generateResult"
        )
        Assert.state(
            generator.isState(HttpGenerator.State.COMPLETING),
            "The HTTP server generator state error. ${generator.state}"
        )
        flushHeaderBuffer()
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

interface Http1ResponseMessage

data class Header(
    val response: MetaData.Response,
    val future: CompletableFuture<Void>
) : Http1ResponseMessage

data class Http1OutputBuffer(
    val buffer: ByteBuffer, val result: Consumer<Result<Int>>
) : Http1ResponseMessage

open class Http1OutputBuffers(
    buffers: Array<ByteBuffer>,
    offset: Int,
    length: Int,
    result: Consumer<Result<Long>>
) : OutputBuffers(buffers, offset, length, result), Http1ResponseMessage

class Http1OutputBufferList(
    bufferList: List<ByteBuffer>,
    offset: Int,
    length: Int,
    result: Consumer<Result<Long>>
) : Http1OutputBuffers(bufferList.toTypedArray(), offset, length, result)

data class EndResponse(val future: CompletableFuture<Void>) : Http1ResponseMessage