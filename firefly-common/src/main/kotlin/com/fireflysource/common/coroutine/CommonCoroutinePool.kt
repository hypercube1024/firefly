package com.fireflysource.common.coroutine

import com.fireflysource.common.concurrent.ExecutorServiceUtils.shutdownAndAwaitTermination
import com.fireflysource.common.coroutine.CoroutineDispatchers.awaitTerminationTimeout
import com.fireflysource.common.ref.Cleaner
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

    val ioBlockingPoolSize: Int =
        Integer.getInteger("com.fireflysource.common.coroutine.ioBlockingPoolSize", availableProcessors * 8)
    val ioBlockingPoolKeepAliveTime: Long =
        Integer.getInteger("com.fireflysource.common.coroutine.ioBlockingPoolKeepAliveTime", 30).toLong()
    val ioBlockingPoolQueueSize: Int =
        Integer.getInteger("com.fireflysource.common.coroutine.ioBlockingPoolQueueSize", 10000)


    val ioBlockingThreadPool: ExecutorService by lazy {
        val threadId = AtomicInteger()
        ThreadPoolExecutor(
            availableProcessors * 2, ioBlockingPoolSize,
            ioBlockingPoolKeepAliveTime, TimeUnit.SECONDS,
            LinkedBlockingQueue(ioBlockingPoolQueueSize)
        ) { runnable -> Thread(runnable, "firefly-io-blocking-pool-" + threadId.getAndIncrement()) }
    }

    val singleThreadPool: ExecutorService by lazy {
        ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS,
            LinkedTransferQueue()
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
            LinkedTransferQueue()
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

    fun newComputationThreadDispatcher(name: String, asyncMode: Boolean = true): CoroutineDispatcher {
        return newComputationThreadExecutor(name, asyncMode).asCoroutineDispatcher()
    }

    fun stopAll() {
        shutdownAndAwaitTermination(computationThreadPool, awaitTerminationTimeout)
        shutdownAndAwaitTermination(singleThreadPool, awaitTerminationTimeout)
        shutdownAndAwaitTermination(ioBlockingThreadPool, awaitTerminationTimeout)
        shutdownAndAwaitTermination(scheduler, awaitTerminationTimeout)
    }
}

val applicationCleaner: Cleaner = Cleaner.create()

class ExecutorCleanTask(private val executor: ExecutorService) : Runnable {
    override fun run() {
        if (!executor.isShutdown) {
            shutdownAndAwaitTermination(executor, awaitTerminationTimeout)
        }
    }
}

class FinalizableExecutorService(private val executor: ExecutorService) : ExecutorService by executor {

    init {
        applicationCleaner.register(this, ExecutorCleanTask(executor))
    }
}

class FinalizableScheduledExecutorService(private val executor: ScheduledExecutorService) :
    ScheduledExecutorService by executor {

    init {
        applicationCleaner.register(this, ExecutorCleanTask(executor))
    }
}

val applicationScope = CoroutineScope(CoroutineName("Firefly-Application"))

inline fun compute(crossinline block: suspend CoroutineScope.() -> Unit): Job =
    applicationScope.launch(CoroutineDispatchers.computation) { block(this) }

inline fun <T> computeAsync(crossinline block: suspend CoroutineScope.() -> T): Deferred<T> =
    applicationScope.async(CoroutineDispatchers.computation) { block(this) }

inline fun blocking(crossinline block: suspend CoroutineScope.() -> Unit): Job =
    applicationScope.launch(CoroutineDispatchers.ioBlocking) { block(this) }

inline fun <T> blockingAsync(crossinline block: suspend CoroutineScope.() -> T): Deferred<T> =
    applicationScope.async(CoroutineDispatchers.ioBlocking) { block(this) }

inline fun event(crossinline block: suspend CoroutineScope.() -> Unit): Job =
    applicationScope.launch(CoroutineDispatchers.singleThread) { block(this) }

inline fun <T> eventAsync(crossinline block: suspend CoroutineScope.() -> T): Deferred<T> =
    applicationScope.async(CoroutineDispatchers.singleThread) { block(this) }

inline fun CoroutineScope.blocking(crossinline block: suspend CoroutineScope.() -> Unit): Job =
    this.launch(CoroutineDispatchers.ioBlocking) { block(this) }

inline fun <T> CoroutineScope.blockingAsync(crossinline block: suspend CoroutineScope.() -> T): Deferred<T> =
    this.async(CoroutineDispatchers.ioBlocking) { block(this) }