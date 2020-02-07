package com.fireflysource.net.tcp.buffer

import com.fireflysource.common.sys.Result
import java.nio.ByteBuffer
import java.util.function.Consumer

sealed class OutputMessage {
    open fun hasRemaining(): Boolean = false
}

class Buffer(val buffer: ByteBuffer, val result: Consumer<Result<Int>>) : OutputMessage() {
    override fun hasRemaining(): Boolean = buffer.hasRemaining()
}

open class Buffers(
    val buffers: Array<ByteBuffer>,
    val offset: Int,
    val length: Int,
    val result: Consumer<Result<Long>>
) : OutputMessage() {

    init {
        require(offset >= 0) { "The offset must be greater than or equal the 0" }
        require(length > 0) { "The length must be greater than 0" }
        require(offset < buffers.size) { "The offset must be less than the buffer size" }
        require((offset + length) <= buffers.size) { "The length must be less than or equal the buffer size" }
    }

    private val maxSize = offset + length
    private val lastIndex = maxSize - 1
    private var currentOffset = offset

    fun getCurrentOffset(): Int {
        for (i in currentOffset..lastIndex) {
            if (buffers[i].hasRemaining()) {
                currentOffset = i
                return i
            }
        }
        return maxSize
    }

    fun getCurrentLength(): Int {
        return maxSize - getCurrentOffset()
    }

    override fun hasRemaining(): Boolean {
        return getCurrentOffset() < maxSize
    }
}

class BufferList(
    bufferList: List<ByteBuffer>,
    offset: Int,
    length: Int,
    result: Consumer<Result<Long>>
) : Buffers(bufferList.toTypedArray(), offset, length, result)

class Shutdown(val result: Consumer<Result<Void>>) : OutputMessage()