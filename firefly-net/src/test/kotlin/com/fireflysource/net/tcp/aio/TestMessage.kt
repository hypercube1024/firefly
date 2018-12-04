package com.fireflysource.net.tcp.aio

import com.fireflysource.net.tcp.BufferList
import com.fireflysource.net.tcp.Buffers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.ByteBuffer
import java.util.function.Consumer

/**
 * @author Pengtao Qiu
 */
class TestMessage {

    @ParameterizedTest
    @CsvSource(value = ["4,2", "2,3", "-1,3", "0,0", "0,-5", "0,6"])
    fun testOutOfBoundException(offset: Int, length: Int) {
        val size = 4
        assertThrows<IllegalArgumentException> {
            Buffers(Array(size) { ByteBuffer.allocate(16) }, offset, length, Consumer {
                println(it)
            })
        }

        assertThrows<IllegalArgumentException> {
            BufferList(List(size) { ByteBuffer.allocate(16) }, offset, length, Consumer {
                println(it)
            })
        }
    }

    @ParameterizedTest
    @CsvSource(value = ["0,3", "1,3", "2,2", "3,1"])
    fun testCurrentOffset(offset: Int, length: Int) {
        val size = 4
        val buffers = Buffers(Array(size) { ByteBuffer.allocate(16) }, offset, length, Consumer {
            println(it)
        })
        buffers.buffers[offset].putInt(1)
        assertEquals(offset, buffers.getCurrentOffset())

        buffers.buffers[offset].putLong(2)
        buffers.buffers[offset].putInt(3)
        assertEquals(offset + 1, buffers.getCurrentOffset())
    }
}