package com.firefly.kotlin.ext.common

import com.firefly.kotlin.ext.log.Log
import kotlinx.coroutines.experimental.*
import org.junit.Test
import java.util.concurrent.*
import kotlin.test.assertEquals

/**
 * @author Pengtao Qiu
 */

private val log = Log.getLogger { }

private val dispatchExecutor: ExecutorService = ThreadPoolExecutor(
        2, 2,
        0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(20),
        Executors.defaultThreadFactory())

// the thread local I want to maintain
private val threadInt = ThreadLocal<Int>()

class TestCoroutineInterceptor {

    @Test
    fun test() {
        runBlocking {
            val dispatcher: CoroutineDispatcher = dispatchExecutor.asCoroutineDispatcher()
            val jobs = List(5) { i ->
                val intContext = InterceptingContext(dispatcher, i, threadInt)

                async(intContext) {
                    log.info("beforeSuspend [local: $i, thread: ${threadInt.get()}]")
                    assertEquals(i, threadInt.get())
                    delay(1000)
                    log.info("afterSuspend [local: $i, thread: ${threadInt.get()}]")
                    assertEquals(i, threadInt.get())
                }
            }

            jobs.forEach {
                it.join()
            }

            log.info("Done")
            delay(2000)
        }
    }
}


