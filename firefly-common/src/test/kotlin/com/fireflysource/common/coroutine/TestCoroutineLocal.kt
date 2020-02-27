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
    private val ctx = CoroutineLocalContext

    @Test
    @DisplayName("should get the coroutine local value across the many coroutines.")
    fun test(): Unit = runBlocking {
        val dispatcher: CoroutineDispatcher = dispatchExecutor.asCoroutineDispatcher()
        val key = "index"
        val jobs = List(5) { i ->
            async(dispatcher + ctx.asElement(mutableMapOf(key to i))) {
                withTimeout(2000) {
                    assertEquals(i, ctx.getAttr<Int>(key))
                    ctx.computeIfAbsent("key33") { 33 }
                    ctx.setAttr("newKey", i)
                    testLocalAttr(key, i)
                    assertEquals(i, ctx.getAttr<Int>(key))
                }
            }
        }

        jobs.forEach {
            it.join()
        }

    }

    private suspend fun testLocalAttr(key: String, expect: Int) = withContextAndInheritParentAttributes {
        assertEquals(33, ctx.getAttr<Int>("key33"))
        assertEquals(expect, ctx.getAttr<Int>("newKey"))
        println("beforeSuspend ${ctx.getAttributes()}")
        launchAndInheritParentAttributes(attributes = mutableMapOf("d1" to 200)) {
            ctx.setAttr("c1", 100)
            assertEquals(100, ctx.getAttr<Int>("c1"))
            assertEquals(expect, ctx.getAttr<Int>(key))
            assertEquals(33, ctx.getAttr<Int>("key33"))
            assertEquals(expect, ctx.getAttr<Int>("newKey"))
            assertEquals("OK", ctx.getAttrOrDefault("keyX") { "OK" })
            println("inner fun [expected: $expect, actual: ${ctx.getAttr<Int>(key)}]")
            assertEquals(200, ctx.getAttr<Int>("d1"))
        }.join()
        println("afterSuspend ${ctx.getAttributes()}")
        assertNull(ctx.getAttr("d1"))
        assertNull(ctx.getAttr("c1"))
    }

    @Test
    @DisplayName("should cancel the channel")
    fun testCancelChannel(): Unit = runBlocking {
        val count = AtomicInteger()
        val channel = Channel<Int>()
        val job = launchJob {
            while (true) {
                val i = channel.receive()
                println("test: $i")
                count.incrementAndGet()
            }
        }

        launchJob {
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