package com.firefly.kotlin.ext.common

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
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
        Executors.newCachedThreadPool { Thread(it, "firefly-io-blocking") }.asCoroutineDispatcher()
    }
}