package com.firefly.kotlin.ext.common

import kotlinx.coroutines.*
import org.junit.Test
import java.util.concurrent.*
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */

private val dispatchExecutor: ExecutorService = ThreadPoolExecutor(
    2, 2,
    0L, TimeUnit.MILLISECONDS,
    ArrayBlockingQueue(20),
    Executors.defaultThreadFactory()
                                                                  )

class TestCoroutineContext {

    @Test
    fun test(): Unit = runBlocking {
        val dispatcher: CoroutineDispatcher = dispatchExecutor.asCoroutineDispatcher()
        val key = "index"
        val jobs = List(5) { i ->
            val e = CoroutineLocalContext.asElement(mutableMapOf(key to i))
            async(dispatcher + e) {
                withTimeout(2000) {
                    println("beforeSuspend [local: $i, thread: ${CoroutineLocalContext.getAttr<Int>(key)}]")
                    assertEquals(i, CoroutineLocalContext.getAttr(key)!!)
                    delay(1000)
                    println("afterSuspend [local: $i, thread: ${CoroutineLocalContext.getAttr<Int>(key)}]")
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
}


