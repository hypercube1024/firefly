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
    val ioBlockingQueueSize = Integer.getInteger(
        "com.fireflysource.common.coroutine.ioBlockingQueueSize",
        20000
                                                )

    val computation: CoroutineDispatcher by lazy {
        ForkJoinPool(defaultPoolSize, { pool ->
            val worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool)
            worker.name = "firefly-computation-pool-" + worker.poolIndex
            worker
        }, null, true).asCoroutineDispatcher()
    }
    val ioBlocking: CoroutineDispatcher by lazy {
        val threadId = AtomicInteger()
        ThreadPoolExecutor(
            defaultPoolSize, 64,
            30L, TimeUnit.SECONDS,
            ArrayBlockingQueue<Runnable>(ioBlockingQueueSize)
                          ) { r ->
            Thread(r, "firefly-io-blocking-pool-" + threadId.getAndIncrement())
        }.asCoroutineDispatcher()
    }
    val singleThread: CoroutineDispatcher by lazy {
        Executors.newSingleThreadExecutor { Thread(it, "firefly-single-thread-pool") }.asCoroutineDispatcher()
    }
}