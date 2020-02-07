package com.fireflysource.net.tcp.aio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * @author Pengtao Qiu
 */
class TestAdaptiveBufferSize {

    @Test
    @DisplayName("should increase or decrease the buffer size when the data size changes.")
    fun test() {
        val bufSize = AdaptiveBufferSize()
        assertEquals(128, bufSize.getBufferSize())

        bufSize.update(1024)
        assertEquals(256, bufSize.getBufferSize())

        bufSize.update(1024)
        assertEquals(512, bufSize.getBufferSize())

        bufSize.update(1024)
        assertEquals(1024, bufSize.getBufferSize())

        bufSize.update(2048)
        assertEquals(2048, bufSize.getBufferSize())

        bufSize.update(4096)
        assertEquals(4096, bufSize.getBufferSize())

        bufSize.update(500)
        assertEquals(2048, bufSize.getBufferSize())

        bufSize.update(100)
        assertEquals(1024, bufSize.getBufferSize())

        repeat(100) { bufSize.update(512 * 1024) }
        assertEquals(512 * 1024, bufSize.getBufferSize())

        repeat(100) { bufSize.update(15) }
        assertEquals(128, bufSize.getBufferSize())
    }
}