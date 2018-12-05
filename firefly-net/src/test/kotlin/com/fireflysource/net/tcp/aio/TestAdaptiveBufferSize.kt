package com.fireflysource.net.tcp.aio

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * @author Pengtao Qiu
 */
class TestAdaptiveBufferSize {

    @Test
    fun test() {
        val bufSize = AdaptiveBufferSize()
        assertEquals(1024, bufSize.getBufferSize())

        bufSize.update(1024)
        assertEquals(2048, bufSize.getBufferSize())

        bufSize.update(1024)
        assertEquals(1024, bufSize.getBufferSize())

        bufSize.update(1024)
        assertEquals(2048, bufSize.getBufferSize())

        bufSize.update(2048)
        assertEquals(4096, bufSize.getBufferSize())
    }
}