package com.fireflysource.common.coroutine

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

/**
 * @author Pengtao Qiu
 */
class TestChannelExtension {

    @Test
    @DisplayName("should poll all elements successfully.")
    fun testPollAll() {
        val channel = Channel<Int>(Channel.UNLIMITED)
        repeat(3) {
            channel.offer(1)
        }
        val list = mutableListOf<Int>()
        channel.pollAll {
            list.add(it)
        }
        assertEquals(3, list.size)
    }

    @Test
    @DisplayName("should clear elements successfully.")
    fun testClear() {
        val channel = Channel<Int>(Channel.UNLIMITED)
        repeat(3) {
            channel.offer(1)
        }
        channel.clear()
        assertNull(channel.poll())
    }

    @Test
    @DisplayName("should send signal successfully.")
    fun testSignal(): Unit = runBlocking {
        val signal = Signal<Int>()
        val result = eventAsync {
            while (true) {
                try {
                    val message = signal.wait()
                    println("received signal $message")
                    if (message == 3) {
                        break
                    }
                } finally {
                    signal.reset()
                }
            }
            "OK"
        }
        event {
            (1..3).forEach {
                delay(TimeUnit.SECONDS.toMillis(1))
                signal.notify(it)
            }
        }
        assertEquals("OK", result.await())
    }

    @Test
    @DisplayName("should wait signal timout.")
    fun testSignalTimeout() {
        val signal = Signal<Int>()
        val result = eventAsync {
            while (true) {
                val message = withTimeout(TimeUnit.SECONDS.toMillis(3)) { signal.wait() }
                println("received signal $message")
                if (message == 3) {
                    break
                }
            }
            "OK"
        }
        event {
            (1..3).forEach {
                signal.notify(it)
            }
        }

        assertThrows(TimeoutCancellationException::class.java) {
            result.asCompletableFuture().get()
        }
    }
}