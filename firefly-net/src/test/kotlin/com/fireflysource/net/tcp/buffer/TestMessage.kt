package com.fireflysource.net.tcp.buffer

import com.fireflysource.common.sys.Result.discard
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.ByteBuffer

/**
 * @author Pengtao Qiu
 */
class TestMessage {

    @ParameterizedTest
    @CsvSource(value = ["4,2", "2,3", "-1,3", "0,0", "0,-5", "0,6"])
    @DisplayName("the length should be less than and equal buffers size subtracts offset")
    fun testOutOfBoundException(offset: Int, length: Int) {
        val size = 4
        assertThrows<IllegalArgumentException> {
            Buffers(Array(size) { ByteBuffer.allocate(16) }, offset, length, discard())
        }

        assertThrows<IllegalArgumentException> {
            BufferList(List(size) { ByteBuffer.allocate(16) }, offset, length, discard())
        }
    }

    @ParameterizedTest
    @CsvSource(value = ["0,3", "1,3", "2,2", "3,1", "0,6", "1,5", "2,3"])
    @DisplayName("the current offset and length should change by the buffers consume")
    fun testCurrentOffsetAndLength(offset: Int, length: Int) {
        val size = 6
        val capacity = 16
        val buffers = Buffers(Array(size) { ByteBuffer.allocate(capacity) }, offset, length, discard())
        buffers.buffers[offset].putInt(1)
        assertEquals(offset, buffers.getCurrentOffset())
        assertEquals(length, buffers.getCurrentLength())

        buffers.buffers[offset].putLong(2)
        buffers.buffers[offset].putInt(3)
        assertEquals(offset + 1, buffers.getCurrentOffset())
        assertEquals(length - 1, buffers.getCurrentLength())
    }

    @ParameterizedTest
    @CsvSource(value = ["0,3", "1,3", "2,2", "3,1", "0,6", "1,5", "2,3"])
    @DisplayName("should not get the remaining")
    fun testHasRemaining(offset: Int, length: Int) {
        val size = 6
        val capacity = 16
        val buffers = Buffers(Array(size) { ByteBuffer.allocate(capacity) }, offset, length, discard())

        val lastIndex = offset + length - 1
        val bytes = ByteArray(capacity)
        (offset..lastIndex).forEach { i -> buffers.buffers[i].put(bytes) }
        assertFalse(buffers.hasRemaining())
        assertEquals(0, buffers.getCurrentLength())
        assertEquals(offset + length, buffers.getCurrentOffset())
    }
}