package com.fireflysource.common.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class TestByteBufferInputStream {

    @Test
    @DisplayName("should read data from temp input stream successfully")
    fun testReadData() {
        val inputStream = ByteBufferInputStream()

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
        var newBuffer = ByteBuffer.wrap(bytes)
        assertEquals(32, newBuffer.remaining())
        (1..8).forEach { assertEquals(it, newBuffer.int) }
        assertEquals(0, inputStream.available())

        buffer = BufferUtils.allocate(16)
        pos = buffer.flipToFill()
        buffer.putInt(9).putInt(10).putInt(11).putInt(12)
        buffer.flipToFlush(pos)
        inputStream.accept(buffer)
        inputStream.accept(BufferUtils.EMPTY_BUFFER)

        bytes = ByteArray(32)
        var len = inputStream.read(bytes)
        newBuffer = ByteBuffer.wrap(bytes, 0, len)
        assertEquals(16, newBuffer.remaining())
        (9..12).forEach { assertEquals(it, newBuffer.int) }
        assertEquals(0, inputStream.available())

        buffer = BufferUtils.allocate(16)
        pos = buffer.flipToFill()
        buffer.putInt(13).putInt(14).putInt(15).putInt(16)
        buffer.flipToFlush(pos)
        inputStream.accept(buffer)

        (13..16).forEach {
            bytes = ByteArray(4)
            len = inputStream.read(bytes)
            newBuffer = ByteBuffer.wrap(bytes, 0, len)
            assertEquals(4, newBuffer.remaining())
            assertEquals(it, newBuffer.int)
            if (it < 16) {
                assertTrue(inputStream.available() > 0)
            }
        }

        bytes = ByteArray(3)
        len = inputStream.read(bytes)
        assertEquals(-1, len)
        inputStream.close()
    }

    @Test
    @DisplayName("should read byte from temp input stream successfully")
    fun testReadByte() {
        val inputStream = ByteBufferInputStream()

        var buffer = BufferUtils.allocate(16)
        var pos = buffer.flipToFill()
        buffer.put(1).put(2).put(3).put(4)
        buffer.flipToFlush(pos)
        inputStream.accept(buffer)
        inputStream.accept(BufferUtils.EMPTY_BUFFER)

        buffer = BufferUtils.allocate(4)
        pos = buffer.flipToFill()
        buffer.put(5).put(6).put(7).put(8)
        buffer.flipToFlush(pos)
        inputStream.accept(buffer)

        (1..8).forEach {
            val b = inputStream.read()
            assertEquals(it, b)
        }

        assertEquals(-1, inputStream.read())
        inputStream.close()
    }

    @Test
    @DisplayName("should throw the EOF exception when the stream is end")
    fun testEof() {
        val inputStream = ByteBufferInputStream()
        assertEquals(-1, inputStream.read())
        inputStream.close()
        assertEquals(-1, inputStream.read())
    }
}