package com.fireflysource.common.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class TestByteBufferTempInputStream {

    @Test
    @DisplayName("should read data from temp input stream successfully")
    fun testReadData() {
        val inputStream = ByteBufferTempInputStream()

        var buffer = BufferUtils.allocate(16)
        var pos = buffer.flipToFill()
        buffer.putInt(1).putInt(2).putInt(3).putInt(4)
        buffer.flipToFlush(pos)
        inputStream.accept(buffer)

        buffer = BufferUtils.allocate(16)
        pos = buffer.flipToFill()
        buffer.putInt(5).putInt(6).putInt(7).putInt(8)
        buffer.flipToFlush(pos)
        inputStream.accept(buffer)

        var bytes = ByteArray(32)
        inputStream.read(bytes)
        val newBuffer = ByteBuffer.wrap(bytes)
        assertEquals(32, newBuffer.remaining())
        (1..8).forEach { assertEquals(it, newBuffer.int) }
        assertEquals(0, inputStream.available())

        bytes = ByteArray(3)
        val len = inputStream.read(bytes)
        assertEquals(-1, len)
    }
}