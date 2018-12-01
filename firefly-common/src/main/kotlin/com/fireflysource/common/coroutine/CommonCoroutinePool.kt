package com.fireflysource.common.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Pengtao Qiu
 */
object CoroutineDispatchers {
    val defaultPoolSize: Int = Integer.getInteger(
        "com.fireflysource.common.coroutine.defaultPoolSize",
        Runtime.getRuntime().availableProcessors()
                                                 )
    val computationThreadPool: ForkJoinPool by lazy {
        ForkJoinPool(defaultPoolSize, { pool ->
            val worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool)
            worker.name = "firefly-computation-pool-" + worker.poolIndex
            worker
        }, null, true)
    }
    val ioBlockingThreadPool: ExecutorService by lazy {
        val threadId = AtomicInteger()
        ThreadPoolExecutor(
            defaultPoolSize, 64,
            30L, TimeUnit.SECONDS,
            ArrayBlockingQueue<Runnable>(20000)
                          ) { r ->
            Thread(r, "firefly-io-blocking-pool-" + threadId.getAndIncrement())
        }
    }
    val computation: CoroutineDispatcher by lazy { computationThreadPool.asCoroutineDispatcher() }
    val ioBlocking: CoroutineDispatcher by lazy { ioBlockingThreadPool.asCoroutineDispatcher() }
}