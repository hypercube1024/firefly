package com.fireflysource.common.io

import java.nio.ByteBuffer

fun ByteBuffer.append(buffer: ByteBuffer): ByteBuffer {
    BufferUtils.append(this, buffer)
    return this
}

fun ByteBuffer.addCapacity(capacity: Int): ByteBuffer {
    return BufferUtils.addCapacity(this, capacity)
}

fun ByteBuffer.flipToFill(): Int = BufferUtils.flipToFill(this)

fun ByteBuffer.flipToFlush(position: Int): ByteBuffer {
    BufferUtils.flipToFlush(this, position)
    return this
}

fun ByteBuffer.copy(): ByteBuffer {
    return if (this.hasRemaining()) BufferUtils.allocate(this.remaining()).append(this)
    else BufferUtils.EMPTY_BUFFER
}