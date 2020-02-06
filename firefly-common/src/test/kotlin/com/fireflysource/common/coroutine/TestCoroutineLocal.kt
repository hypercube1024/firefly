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
            val e = CoroutineLocalContext.asElement(mutableMapOf(key to i))
            async(dispatcher + e) {
                withTimeout(2000) {
                    println("beforeSuspend [expected: $i, actual: ${CoroutineLocalContext.getAttr<Int>(key)}]")
                    assertEquals(i, CoroutineLocalContext.getAttr(key)!!)
                    CoroutineLocalContext.computeIfAbsent("constAttr") { 33 }
                    CoroutineLocalContext.setAttr("e", i)
                    delay(100)
                    testLocalAttr(key, i)
                    println("afterSuspend [expected: $i, actual: ${CoroutineLocalContext.getAttr<Int>(key)}]")
                    assertEquals(i, CoroutineLocalContext.getAttr(key)!!)
                }
            }
        }

        jobs.forEach {
            it.join()
        }

        println("Done")
        delay(2000)
    }

    private suspend fun testLocalAttr(key: String, i: Int) = withAttr {
        launchWithAttr {
            delay(100)
            assertEquals(i, CoroutineLocalContext.getAttr(key)!!)
            assertEquals(33, CoroutineLocalContext.getAttr("constAttr")!!)
            assertEquals(i, CoroutineLocalContext.getAttr("e")!!)
            println("inner fun [expected: $i, actual: ${CoroutineLocalContext.getAttr<Int>(key)}]")
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