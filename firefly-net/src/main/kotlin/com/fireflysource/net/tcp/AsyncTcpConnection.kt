package com.fireflysource.net.tcp

import kotlinx.coroutines.channels.Channel
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.util.function.Consumer

/**
 * @author Pengtao Qiu
 */


interface AsyncTcpConnection : TcpConnection {

    suspend fun asyncWrite(byteBuffer: ByteBuffer): Int

    suspend fun asyncWrite(byteBuffers: Array<ByteBuffer>, offset: Int, length: Int): Long

    suspend fun asyncWrite(byteBufferList: List<ByteBuffer>, offset: Int, length: Int): Long

    fun getInputChannel(): Channel<ByteBuffer>

}

class CloseRequestException : IllegalStateException("The close request has been sent")

class ChannelClosedException : IllegalStateException("The socket channel is closed")

sealed class Message
class Buffer(val buffer: ByteBuffer, val result: Consumer<Result<Int>>) : Message()
class Buffers(
    val buffers: Array<ByteBuffer>,
    val offset: Int,
    val length: Int,
    val result: Consumer<Result<Long>>
             ) : Message() {

    init {
        require(offset >= 0) { "The offset must be greater than or equal the 0" }
        require(length > 0) { "The length must be greater than 0" }
        require(offset < buffers.size) { "The offset must be less than the buffer array size" }
        require((offset + length) <= buffers.size) { "The length must be less than or equal the buffer array size" }
    }

    fun getCurrentOffset(): Int {
        buffers.forEachIndexed { index, byteBuffer ->
            if (index >= offset && byteBuffer.hasRemaining()) {
                return index
            }
        }
        return buffers.size
    }

    fun getCurrentLength(): Int = buffers.size - getCurrentOffset()

    fun hasRemaining(): Boolean = getCurrentOffset() < buffers.size
}

class BufferList(
    val bufferList: List<ByteBuffer>,
    val offset: Int,
    val length: Int,
    val result: Consumer<Result<Long>>
                ) : Message() {

    init {
        require(offset >= 0) { "The offset must be greater than or equal the 0" }
        require(length > 0) { "The length must be greater than 0" }
        require(offset < bufferList.size) { "The offset must be less than the buffer list size" }
        require((offset + length) <= bufferList.size) { "The length must be less than or equal the buffer list size" }
    }

    fun getCurrentOffset(): Int {
        bufferList.forEachIndexed { index, byteBuffer ->
            if (index >= offset && byteBuffer.hasRemaining()) {
                return index
            }
        }
        return bufferList.size
    }

    fun getCurrentLength(): Int = bufferList.size - getCurrentOffset()

    fun hasRemaining(): Boolean = getCurrentOffset() < bufferList.size
}

object Shutdown : Message()