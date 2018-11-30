package com.firefly.kotlin.ext.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Pengtao Qiu
 */
object CoroutineDispatchers {
    val defaultPoolSize: Int = Integer.getInteger(
        "com.firefly.kotlin.common.async.defaultPoolSize",
        Runtime.getRuntime().availableProcessors()
                                                 )
    val computationThreadPool: ForkJoinPool by lazy {
        ForkJoinPool(defaultPoolSize, { pool ->
            val worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool)
            worker.name = "firefly-kt-async-pool-" + worker.poolIndex
            worker
        }, null, true)
    }
    val ioBlockingThreadPool: ExecutorService by lazy {
        val threadId = AtomicInteger()
        ThreadPoolExecutor(
            16, 64,
            30L, TimeUnit.SECONDS,
            ArrayBlockingQueue<Runnable>(10000)
                          ) { r ->
            Thread(r, "firefly-kt-io-blocking-pool-" + threadId.getAndIncrement())
        }
    }
    val computation: CoroutineDispatcher by lazy { computationThreadPool.asCoroutineDispatcher() }
    val ioBlocking: CoroutineDispatcher by lazy { ioBlockingThreadPool.asCoroutineDispatcher() }
}