package com.fireflysource.net.tcp.aio

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.buffer.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.function.Consumer

class BufferedOutputTcpConnection(
    private val tcpConnection: TcpConnection,
    private val bufferSize: Int = 8192
) : TcpConnection by tcpConnection {

    companion object {
        private val log = SystemLogger.create(BufferedOutputTcpConnection::class.java)
    }

    private val buffer: ByteBuffer = BufferUtils.allocateDirect(bufferSize)
    private var position = buffer.flipToFill()
    private val outputMessageChannel: Channel<OutputMessage> = Channel(Channel.UNLIMITED)

    init {
        flushJob()
    }

    private fun flushJob() = coroutineScope.launch {
        flushLoop@ while (true) {
            when (val message = outputMessageChannel.receive()) {
                is OutputBuffer -> flushOutputBuffer(message)
                is OutputBuffers -> flushOutputBuffers(message)
                is OutputBufferList -> flushOutputBuffers(message)
                is FlushOutput -> flushBuffer()
                is ShutdownOutput -> {
                    flushBuffer()
                    tcpConnection.close(message.result)
                    break@flushLoop
                }
            }
        }
    }

    private suspend fun flushOutputBuffer(message: OutputBuffer) {
        try {
            val remaining = message.buffer.remaining()
            append(message.buffer)
            message.result.accept(Result(true, remaining, null))
        } catch (e: Exception) {
            message.result.accept(Result(false, -1, e))
        }
    }

    private suspend fun flushOutputBuffers(message: OutputBuffers) {
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
            log.debug { "Flush buffer. id: $id, src: $srcRemaining, consumed: $consumed" }
            if (!buffer.hasRemaining()) {
                flushBuffer()
            }
        }
    }

    private suspend fun flushBuffer() {
        buffer.flipToFlush(position)
        tcpConnection.write(buffer).await()
        BufferUtils.clear(buffer)
        position = buffer.flipToFill()
    }

    override fun flush(): TcpConnection {
        outputMessageChannel.offer(FlushOutput)
        return this
    }

    override fun getBufferSize(): Int = bufferSize

    override fun write(byteBuffer: ByteBuffer, result: Consumer<Result<Int>>): TcpConnection {
        outputMessageChannel.offer(OutputBuffer(byteBuffer, result))
        return this
    }

    override fun write(
        byteBuffers: Array<ByteBuffer>, offset: Int, length: Int, result: Consumer<Result<Long>>
    ): TcpConnection {
        val message = OutputBuffers(byteBuffers, offset, length, result)
        outputMessageChannel.offer(message)
        return this
    }

    override fun write(
        byteBufferList: List<ByteBuffer>, offset: Int, length: Int, result: Consumer<Result<Long>>
    ): TcpConnection {
        val message = OutputBufferList(byteBufferList, offset, length, result)
        outputMessageChannel.offer(message)
        return this
    }

    override fun close(result: Consumer<Result<Void>>): TcpConnection {
        outputMessageChannel.offer(ShutdownOutput(result))
        return this
    }
}