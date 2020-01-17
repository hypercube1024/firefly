package com.fireflysource.common.coroutine

import com.fireflysource.common.concurrent.ExecutorServiceUtils.shutdownAndAwaitTermination
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

/**
 * @author Pengtao Qiu
 */
object CoroutineDispatchers {

    val defaultPoolSize: Int = Integer.getInteger(
        "com.fireflysource.common.coroutine.defaultPoolSize",
        Runtime.getRuntime().availableProcessors()
    )
    val defaultPoolKeepAliveTime: Long =
        Integer.getInteger("com.fireflysource.common.coroutine.defaultPoolKeepAliveTime", 30).toLong()

    val ioBlockingQueueSize: Int =
        Integer.getInteger("com.fireflysource.common.coroutine.ioBlockingQueueSize", 16 * 1024)
    val ioBlockingPoolSize: Int = Integer.getInteger(
        "com.fireflysource.common.coroutine.ioBlockingPoolSize",
        max(32, Runtime.getRuntime().availableProcessors())
    )
    val ioBlockingPoolKeepAliveTime: Long =
        Integer.getInteger("com.fireflysource.common.coroutine.ioBlockingPoolKeepAliveTime", 30).toLong()


    val ioBlockingThreadPool: ExecutorService by lazy {
        val threadId = AtomicInteger()
        ThreadPoolExecutor(
            defaultPoolSize, ioBlockingPoolSize,
            ioBlockingPoolKeepAliveTime, TimeUnit.SECONDS,
            ArrayBlockingQueue<Runnable>(ioBlockingQueueSize)
        ) { runnable -> Thread(runnable, "firefly-io-blocking-pool-" + threadId.getAndIncrement()) }
    }

    val singleThreadPool: ExecutorService by lazy {
        ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS,
            LinkedTransferQueue<Runnable>()
        ) { runnable -> Thread(runnable, "firefly-single-thread-pool") }
    }

    val computationThreadPool: ExecutorService by lazy {
        ForkJoinPool(defaultPoolSize, { pool ->
            val worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool)
            worker.name = "firefly-computation-pool-" + worker.poolIndex
            worker
        }, null, true)
    }


    val computation: CoroutineDispatcher by lazy { computationThreadPool.asCoroutineDispatcher() }
    val ioBlocking: CoroutineDispatcher by lazy { ioBlockingThreadPool.asCoroutineDispatcher() }
    val singleThread: CoroutineDispatcher by lazy { singleThreadPool.asCoroutineDispatcher() }

    val scheduler: ScheduledExecutorService by lazy {
        Executors.newScheduledThreadPool(defaultPoolSize) {
            Thread(it, "firefly-scheduler-thread")
        }
    }

    fun newSingleThreadExecutor(name: String): ExecutorService {
        val executor = ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS,
            LinkedTransferQueue<Runnable>()
        ) { runnable -> Thread(runnable, name) }
        return FinalizableExecutorService(executor)
    }

    fun newFixedThreadExecutor(name: String, poolSize: Int): ExecutorService {
        require(poolSize > 0)
        val executor = ThreadPoolExecutor(
            min(defaultPoolSize, poolSize),
            max(defaultPoolSize, poolSize),
            defaultPoolKeepAliveTime, TimeUnit.SECONDS,
            LinkedTransferQueue<Runnable>()
        ) { runnable -> Thread(runnable, name) }
        return FinalizableExecutorService(executor)
    }

    fun newComputationThreadExecutor(name: String, asyncMode: Boolean = true): ExecutorService {
        val executor = ForkJoinPool(defaultPoolSize, { pool ->
            val worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool)
            worker.name = name + "-" + worker.poolIndex
            worker
        }, null, asyncMode)
        return FinalizableExecutorService(executor)
    }

    fun newSingleThreadDispatcher(name: String): CoroutineDispatcher {
        return newSingleThreadExecutor(name).asCoroutineDispatcher()
    }

    fun newFixedThreadDispatcher(name: String, poolSize: Int): CoroutineDispatcher {
        return newFixedThreadExecutor(name, poolSize).asCoroutineDispatcher()
    }

    fun stopAll() {
        shutdownAndAwaitTermination(computationThreadPool, 5, TimeUnit.SECONDS)
        shutdownAndAwaitTermination(singleThreadPool, 5, TimeUnit.SECONDS)
        shutdownAndAwaitTermination(ioBlockingThreadPool, 5, TimeUnit.SECONDS)
        shutdownAndAwaitTermination(scheduler, 5, TimeUnit.SECONDS)
    }
}

class FinalizableExecutorService(private val executor: ExecutorService) : ExecutorService by executor,
    AbstractLifeCycle() {

    init {
        start()
    }

    override fun init() {
    }

    override fun destroy() {
        if (!executor.isShutdown) {
            shutdownAndAwaitTermination(executor, 5, TimeUnit.SECONDS)
        }
    }

    protected fun finalize() {
        if (!executor.isShutdown) {
            shutdownAndAwaitTermination(executor, 5, TimeUnit.SECONDS)
        }
    }
}

class FinalizableScheduledExecutorService(private val executor: ScheduledExecutorService) :
    ScheduledExecutorService by executor, AbstractLifeCycle() {
    init {
        start()
    }

    override fun init() {
    }

    override fun destroy() {
        if (!executor.isShutdown) {
            shutdownAndAwaitTermination(executor, 5, TimeUnit.SECONDS)
        }
    }

    protected fun finalize() {
        if (!executor.isShutdown) {
            shutdownAndAwaitTermination(executor, 5, TimeUnit.SECONDS)
        }
    }
}