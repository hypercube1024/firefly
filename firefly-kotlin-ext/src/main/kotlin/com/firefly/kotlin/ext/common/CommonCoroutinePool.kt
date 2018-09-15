package com.firefly.kotlin.ext.common

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.experimental.CoroutineContext

/**
 * @author Pengtao Qiu
 */
object CommonCoroutinePool : CoroutineDispatcher() {

    val defaultPoolSize: Int = Integer.getInteger("com.firefly.kotlin.common.async.defaultPoolSize", Runtime.getRuntime().availableProcessors())

    private val pool: ForkJoinPool = ForkJoinPool(defaultPoolSize, { pool ->
        val worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool)
        worker.name = "firefly-kt-async-pool-" + worker.poolIndex
        worker
    }, null, true)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        pool.execute(block)
    }

}

object CoroutineDispatchers {
    val computation: CommonCoroutinePool by lazy { CommonCoroutinePool }
    val ioBlocking: CoroutineDispatcher by lazy {
        val threadId = AtomicInteger()
        ThreadPoolExecutor(16, 64,
                30L, TimeUnit.SECONDS,
                ArrayBlockingQueue<Runnable>(10000)
        ) { r -> Thread(r, "firefly-kt-io-blocking-pool-" + threadId.getAndIncrement()) }.asCoroutineDispatcher()
    }
}