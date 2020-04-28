package com.fireflysource.common.io

import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

class ByteBufferTempInputStream : InputStream(), Consumer<ByteBuffer> {

    private val buffers: LinkedList<ByteBuffer> = LinkedList()
    private val closed = AtomicBoolean(false)

    @ExperimentalUnsignedTypes
    override fun read(): Int {
        check()

        fun peekBuffer(): ByteBuffer {
            while (buffers.isNotEmpty()) {
                val buffer = buffers.peek()
                if (buffer.hasRemaining()) {
                    return@peekBuffer buffer
                } else {
                    buffers.poll()
                }
            }
            throw EmptyBufferException("The temp buffer is empty.")
        }

        val buffer = peekBuffer()
        val b = buffer.get().toUByte().toInt()
        if (!buffer.hasRemaining()) {
            buffers.poll()
        }
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        require(off >= 0) { "The offset must be greater than or equals 0." }
        require(len <= b.size - off) { "The length must be less than or equals the array size." }

        check()

        val remaining = b.size - off
        val maxLength = remaining.coerceAtMost(len)
        var position = off
        var count = 0
        var remainingBytes = maxLength

        while (buffers.isNotEmpty()) {
            val buffer = buffers.peek()
            if (buffer.hasRemaining()) {
                val size = buffer.remaining().coerceAtMost(remainingBytes)
                buffer.get(b, position, size)

                count += size
                remainingBytes -= size
                position += size

                if (!buffer.hasRemaining()) {
                    buffers.poll()
                }

                if (remainingBytes == 0) {
                    return count
                }
            } else {
                buffers.poll()
            }
        }
        return count
    }

    override fun available(): Int {
        return if (buffers.isEmpty()) 0
        else buffers.sumBy { it.remaining() }
    }

    override fun accept(buffer: ByteBuffer) {
        buffers.offer(buffer)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            buffers.clear()
        }
    }

    private fun check() {
        if (closed.get()) throw EOFException("The stream is closed")

        if (buffers.isEmpty()) throw EmptyBufferException("The temp buffer is empty.")
    }
}