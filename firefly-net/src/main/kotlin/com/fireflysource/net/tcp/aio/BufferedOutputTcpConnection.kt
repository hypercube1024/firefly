package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.consumeAll
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.WrappedTcpConnection
import com.fireflysource.net.tcp.buffer.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.util.function.Consumer

class BufferedOutputTcpConnection(
    private val tcpConnection: TcpConnection,
    private val bufferSize: Int = 8192
) : TcpConnection by tcpConnection, WrappedTcpConnection {

    companion object {
        private val log = SystemLogger.create(BufferedOutputTcpConnection::class.java)
    }

    private val buffer: ByteBuffer = BufferUtils.allocateDirect(bufferSize)
    private var position = buffer.flipToFill()
    private val outputMessageChannel: Channel<OutputMessage> = Channel(Channel.UNLIMITED)

    init {
        flushJob()
    }

    override fun getRawTcpConnection(): TcpConnection = tcpConnection

    private fun flushJob() = coroutineScope.launch {
        while (true) {
            when (val message = outputMessageChannel.receive()) {
                is OutputBuffer -> appendOutputBuffer(message)
                is OutputBuffers -> appendOutputBuffers(message)
                is OutputBufferList -> appendOutputBuffers(message)
                is FlushOutput -> flushBuffer(message)
                is ShutdownOutput -> {
                    shutdownOutput(message)
                    break
                }
            }
        }
    }.invokeOnCompletion { cause ->
        val e = cause ?: ClosedChannelException()
        outputMessageChannel.consumeAll { message ->
            when (message) {
                is OutputBuffer -> message.result.accept(Result.createFailedResult(-1, e))
                is OutputBuffers -> message.result.accept(Result.createFailedResult(-1, e))
                is OutputBufferList -> message.result.accept(Result.createFailedResult(-1, e))
                is ShutdownOutput -> message.result.accept(Result.createFailedResult(e))
                is FlushOutput -> message.result.accept(Result.createFailedResult(e))
                else -> {
                }
            }
        }
    }

    private suspend fun appendOutputBuffer(message: OutputBuffer) {
        try {
            val remaining = message.buffer.remaining()
            append(message.buffer)
            message.result.accept(Result(true, remaining, null))
        } catch (e: Exception) {
            message.result.accept(Result(false, -1, e))
        }
    }

    private suspend fun appendOutputBuffers(message: OutputBuffers) {
        try {
            val remaining = message.remaining()
            val offset = message.getCurrentOffset()
            val lastIndex = message.getLastIndex()
            (offset..lastIndex).map { message.buffers[it] }.forEach { append(it) }
            message.result.accept(Result(true, remaining, null))
        } catch (e: Exception) {
            message.result.accept(Result(false, -1, e))
        }
    }

    private suspend fun append(src: ByteBuffer) {
        while (src.hasRemaining()) {
            val srcRemaining = src.remaining()
            val consumed = BufferUtils.put(src, buffer)
            log.debug { "Append buffer. id: $id, src: $srcRemaining, consumed: $consumed" }
            if (!buffer.hasRemaining()) {
                flushBuffer()
            }
        }
    }

    private suspend fun flushBuffer(message: FlushOutput) {
        try {
            flushBuffer()
            message.result.accept(Result.SUCCESS)
        } catch (e: Exception) {
            message.result.accept(Result.createFailedResult(e))
        }
    }

    private suspend fun flushBuffer() {
        buffer.flipToFlush(position)
        val remaining = buffer.remaining()
        val consumed = tcpConnection.write(buffer).await()
        log.debug { "Flush buffer. id: $id, len: $remaining, consumed: $consumed" }
        BufferUtils.clear(buffer)
        position = buffer.flipToFill()
    }

    private suspend fun shutdownOutput(message: ShutdownOutput) {
        try {
            flushBuffer()
            tcpConnection.close(message.result)
        } catch (e: Exception) {
            message.result.accept(Result.createFailedResult(e))
        }
    }

    override fun flush(result: Consumer<Result<Void>>): TcpConnection {
        outputMessageChannel.trySend(FlushOutput(result))
        return this
    }

    override fun getBufferSize(): Int = bufferSize

    override fun write(byteBuffer: ByteBuffer, result: Consumer<Result<Int>>): TcpConnection {
        outputMessageChannel.trySend(OutputBuffer(byteBuffer, result))
        return this
    }

    override fun write(
        byteBuffers: Array<ByteBuffer>, offset: Int, length: Int, result: Consumer<Result<Long>>
    ): TcpConnection {
        val message = OutputBuffers(byteBuffers, offset, length, result)
        outputMessageChannel.trySend(message)
        return this
    }

    override fun write(
        byteBufferList: List<ByteBuffer>, offset: Int, length: Int, result: Consumer<Result<Long>>
    ): TcpConnection {
        val message = OutputBufferList(byteBufferList, offset, length, result)
        outputMessageChannel.trySend(message)
        return this
    }

    override fun close(result: Consumer<Result<Void>>): TcpConnection {
        outputMessageChannel.trySend(ShutdownOutput(result))
        return this
    }
}