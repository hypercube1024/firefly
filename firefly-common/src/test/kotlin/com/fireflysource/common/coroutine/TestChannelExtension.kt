package com.fireflysource.common.coroutine

import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * @author Pengtao Qiu
 */
class TestChannelExtension {

    @Test
    @DisplayName("should poll all elements successfully.")
    fun testPollAll() {
        val channel = Channel<Int>(Channel.UNLIMITED)
        repeat(3) {
            channel.trySend(1)
        }
        val list = mutableListOf<Int>()
        channel.consumeAll {
            list.add(it)
        }
        assertEquals(3, list.size)
    }

    @Test
    @DisplayName("should clear elements successfully.")
    fun testClear() {
        val channel = Channel<Int>(Channel.UNLIMITED)
        repeat(3) {
            channel.trySend(1)
        }
        channel.clear()
        assertNull(channel.tryReceive().getOrNull())
    }
}