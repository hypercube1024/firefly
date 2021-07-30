package com.fireflysource.net.tcp.buffer

import com.fireflysource.common.sys.Result
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

sealed interface InputMessage

@JvmInline
value class InputBuffer(val bufferFuture: CompletableFuture<ByteBuffer>) : InputMessage

object ShutdownInput : InputMessage


sealed class OutputMessage {
    open fun hasRemaining(): Boolean = false
}

data class OutputBuffer(val buffer: ByteBuffer, val result: Consumer<Result<Int>>) : OutputMessage() {
    override fun hasRemaining(): Boolean = buffer.hasRemaining()
}

open class OutputBuffers(
    val buffers: Array<ByteBuffer>,
    val offset: Int,
    val length: Int,
    val result: Consumer<Result<Long>>,
    private val outputBufferArray: DelegatedOutputBufferArray = DelegatedOutputBufferArray(
        buffers, offset, length, result
    )
) : OutputMessage(), OutputBufferArray by outputBufferArray {
    override fun hasRemaining(): Boolean {
        return outputBufferArray.hasRemaining()
    }
}

class OutputBufferList(
    bufferList: List<ByteBuffer>,
    offset: Int,
    length: Int,
    result: Consumer<Result<Long>>
) : OutputBuffers(bufferList.toTypedArray(), offset, length, result)

class ShutdownOutput(val result: Consumer<Result<Void>>) : OutputMessage()

class FlushOutput(val result: Consumer<Result<Void>>) : OutputMessage()


interface OutputBufferArray {
    fun getCurrentOffset(): Int
    fun getCurrentLength(): Int
    fun getLastIndex(): Int
    fun remaining(): Long
    fun hasRemaining(): Boolean
}

class DelegatedOutputBufferArray(
    val buffers: Array<ByteBuffer>,
    val offset: Int,
    val length: Int,
    val result: Consumer<Result<Long>>
) : OutputBufferArray {
    init {
        require(offset >= 0) { "The offset must be greater than or equal the 0" }
        require(length > 0) { "The length must be greater than 0" }
        require(offset < buffers.size) { "The offset must be less than the buffer size" }
        require((offset + length) <= buffers.size) { "The length must be less than or equal the buffer size" }
    }

    private val maxSize = offset + length
    private val lastIndex = maxSize - 1
    private var currentOffset = offset

    override fun getCurrentOffset(): Int {
        for (i in currentOffset..lastIndex) {
            if (buffers[i].hasRemaining()) {
                currentOffset = i
                return i
            }
        }
        return maxSize
    }

    override fun getCurrentLength(): Int {
        return maxSize - getCurrentOffset()
    }

    override fun getLastIndex(): Int = lastIndex

    override fun remaining(): Long {
        val offset = getCurrentOffset()
        return (offset..lastIndex).sumOf { buffers[it].remaining().toLong() }
    }

    override fun hasRemaining(): Boolean {
        return getCurrentOffset() < maxSize
    }
}