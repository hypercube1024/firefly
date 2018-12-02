package com.fireflysource.common.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
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
    val singleThreadQueueSize = Integer.getInteger(
        "com.fireflysource.common.coroutine.singleThreadQueueSize",
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
        val threadId = AtomicInteger()
        ThreadPoolExecutor(
            1, 1,
            0, TimeUnit.MILLISECONDS,
            ArrayBlockingQueue<Runnable>(singleThreadQueueSize)
                          ) { r ->
            Thread(r, "firefly-single-thread-pool-" + threadId.getAndIncrement())
        }.asCoroutineDispatcher()
    }
}