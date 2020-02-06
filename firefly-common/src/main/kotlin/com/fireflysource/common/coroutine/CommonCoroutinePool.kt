package com.fireflysource.common.coroutine

import com.fireflysource.common.concurrent.ExecutorServiceUtils.shutdownAndAwaitTermination
import com.fireflysource.common.coroutine.CoroutineDispatchers.awaitTerminationTimeout
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import kotlinx.coroutines.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Pengtao Qiu
 */
object CoroutineDispatchers {

    val availableProcessors = Runtime.getRuntime().availableProcessors()
    val awaitTerminationTimeout =
        Integer.getInteger("com.fireflysource.common.coroutine.awaitTerminationTimeout", 5).toLong()

    val defaultPoolSize: Int =
        Integer.getInteger("com.fireflysource.common.coroutine.defaultPoolSize", availableProcessors)
    val defaultPoolKeepAliveTime: Long =
        Integer.getInteger("com.fireflysource.common.coroutine.defaultPoolKeepAliveTime", 30).toLong()

    val ioBlockingPoolSize: Int = Integer.getInteger(
        "com.fireflysource.common.coroutine.ioBlockingPoolSize",
        32.coerceAtLeast(availableProcessors)
    )
    val ioBlockingPoolKeepAliveTime: Long =
        Integer.getInteger("com.fireflysource.common.coroutine.ioBlockingPoolKeepAliveTime", 30).toLong()


    val ioBlockingThreadPool: ExecutorService by lazy {
        val threadId = AtomicInteger()
        ThreadPoolExecutor(
            availableProcessors, ioBlockingPoolSize,
            ioBlockingPoolKeepAliveTime, TimeUnit.SECONDS,
            LinkedTransferQueue<Runnable>()
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

    val supervisor: CompletableJob = SupervisorJob()
    val computationScope: CoroutineScope = CoroutineScope(computation + supervisor)

    fun newSingleThreadExecutor(name: String): ExecutorService {
        val executor = ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS,
            LinkedTransferQueue<Runnable>()
        ) { runnable -> Thread(runnable, name) }
        return FinalizableExecutorService(executor)
    }

    fun newFixedThreadExecutor(name: String, poolSize: Int, maxPoolSize: Int = poolSize): ExecutorService {
        val executor = ThreadPoolExecutor(
            poolSize,
            maxPoolSize,
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

    fun newFixedThreadDispatcher(name: String, poolSize: Int, maxPoolSize: Int = poolSize): CoroutineDispatcher {
        return newFixedThreadExecutor(name, poolSize, maxPoolSize).asCoroutineDispatcher()
    }

    fun newComputationThreadDispatcher(name: String, asyncMode: Boolean = true): CoroutineDispatcher {
        return newComputationThreadExecutor(name, asyncMode).asCoroutineDispatcher()
    }

    fun stopAll() {
        shutdownAndAwaitTermination(computationThreadPool, awaitTerminationTimeout, TimeUnit.SECONDS)
        shutdownAndAwaitTermination(singleThreadPool, awaitTerminationTimeout, TimeUnit.SECONDS)
        shutdownAndAwaitTermination(ioBlockingThreadPool, awaitTerminationTimeout, TimeUnit.SECONDS)
        shutdownAndAwaitTermination(scheduler, awaitTerminationTimeout, TimeUnit.SECONDS)
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
            shutdownAndAwaitTermination(executor, awaitTerminationTimeout, TimeUnit.SECONDS)
        }
    }

    protected fun finalize() {
        if (!executor.isShutdown) {
            shutdownAndAwaitTermination(executor, awaitTerminationTimeout, TimeUnit.SECONDS)
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
            shutdownAndAwaitTermination(executor, awaitTerminationTimeout, TimeUnit.SECONDS)
        }
    }

    protected fun finalize() {
        if (!executor.isShutdown) {
            shutdownAndAwaitTermination(executor, awaitTerminationTimeout, TimeUnit.SECONDS)
        }
    }
}