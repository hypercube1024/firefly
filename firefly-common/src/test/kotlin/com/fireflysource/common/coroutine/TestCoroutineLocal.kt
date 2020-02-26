package com.fireflysource.common.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
                    println("beforeSuspend [expected: $i, actual: ${CoroutineLocalContext.getAttr<Int>(key)}]")
                    assertEquals(i, CoroutineLocalContext.getAttr<Int>(key))
                    CoroutineLocalContext.computeIfAbsent("key33") { 33 }
                    CoroutineLocalContext.setAttr("newKey", i)
                    delay(100)
                    testLocalAttr(key, i)
                    println("afterSuspend [expected: $i, actual: ${CoroutineLocalContext.getAttr<Int>(key)}]")
                    assertEquals(i, CoroutineLocalContext.getAttr<Int>(key))
                }
            }
        }

        jobs.forEach {
            it.join()
        }
        println("Done")
    }

    private suspend fun testLocalAttr(key: String, expect: Int) = withAttributes {
        launchWithAttributes {
            delay(100)
            assertEquals(expect, CoroutineLocalContext.getAttr<Int>(key))
            assertEquals(33, CoroutineLocalContext.getAttr<Int>("key33"))
            assertEquals(expect, CoroutineLocalContext.getAttr<Int>("newKey"))
            assertEquals("OK", CoroutineLocalContext.getAttrOrDefault("keyX") { "OK" })
            println("inner fun [expected: $expect, actual: ${CoroutineLocalContext.getAttr<Int>(key)}]")
        }.join()
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