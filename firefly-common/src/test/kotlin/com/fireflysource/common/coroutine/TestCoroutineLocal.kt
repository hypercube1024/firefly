package com.fireflysource.common.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Pengtao Qiu
 */

private val dispatchExecutor: ExecutorService = ThreadPoolExecutor(
    2, 2,
    0L, TimeUnit.MILLISECONDS,
    ArrayBlockingQueue(20),
    Executors.defaultThreadFactory()
)

class TestCoroutineLocal {

    @Test
    @DisplayName("should get the coroutine local value across the many coroutines.")
    fun test(): Unit = runBlocking {
        val dispatcher: CoroutineDispatcher = dispatchExecutor.asCoroutineDispatcher()
        val key = "index"
        val jobs = List(5) { i ->
            async(dispatcher + CoroutineLocalContext.asElement(mutableMapOf(key to i))) {
                withTimeout(2000) {
                    assertEquals(i, CoroutineLocalContext.getAttr<Int>(key))
                    CoroutineLocalContext.computeIfAbsent("key33") { 33 }
                    CoroutineLocalContext.setAttr("newKey", i)
                    testLocalAttr(key, i)
                    assertEquals(i, CoroutineLocalContext.getAttr<Int>(key))
                }
            }
        }

        jobs.forEach {
            it.join()
        }

    }

    private suspend fun testLocalAttr(key: String, expect: Int) = withAttributes {
        assertEquals(33, CoroutineLocalContext.getAttr<Int>("key33"))
        assertEquals(expect, CoroutineLocalContext.getAttr<Int>("newKey"))
        println("beforeSuspend ${CoroutineLocalContext.getAttributes()}")
        launchWithAttributes(attributes = mutableMapOf("d1" to 200)) {
            CoroutineLocalContext.setAttr("c1", 100)
            assertEquals(100, CoroutineLocalContext.getAttr<Int>("c1"))
            assertEquals(expect, CoroutineLocalContext.getAttr<Int>(key))
            assertEquals(33, CoroutineLocalContext.getAttr<Int>("key33"))
            assertEquals(expect, CoroutineLocalContext.getAttr<Int>("newKey"))
            assertEquals("OK", CoroutineLocalContext.getAttrOrDefault("keyX") { "OK" })
            println("inner fun [expected: $expect, actual: ${CoroutineLocalContext.getAttr<Int>(key)}]")
            assertEquals(200, CoroutineLocalContext.getAttr<Int>("d1"))
        }.join()
        println("afterSuspend ${CoroutineLocalContext.getAttributes()}")
        assertNull(CoroutineLocalContext.getAttr("d1"))
        assertNull(CoroutineLocalContext.getAttr("c1"))
    }

    @Test
    @DisplayName("should cancel the channel")
    fun testCancelChannel(): Unit = runBlocking {
        val count = AtomicInteger()
        val channel = Channel<Int>()
        val job = launchTask {
            while (true) {
                val i = channel.receive()
                println("test: $i")
                count.incrementAndGet()
            }
        }

        launchTask {
            (1..10).forEach {
                delay(100)
                channel.offer(it)
            }
        }

        delay(500)
        job.cancel()
        job.join()
        println("end")
        assertTrue(count.get() < 10)
    }
}