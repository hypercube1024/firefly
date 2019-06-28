package com.fireflysource.common.coroutine

import com.fireflysource.common.concurrent.ExecutorServiceUtils
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
    val ioBlockingQueueSize = Integer.getInteger("com.fireflysource.common.coroutine.ioBlockingQueueSize", 20000)
    val ioBlockingPoolSize: Int = Integer.getInteger("com.fireflysource.common.coroutine.ioBlockingPoolSize", 64)


    val ioBlockingPool: ExecutorService by lazy {
        val threadId = AtomicInteger()
        FinalizableExecutorService(ThreadPoolExecutor(
            defaultPoolSize, ioBlockingPoolSize,
            30L, TimeUnit.SECONDS,
            ArrayBlockingQueue<Runnable>(ioBlockingQueueSize)
        ) { r ->
            Thread(r, "firefly-io-blocking-pool-" + threadId.getAndIncrement())
        })
    }
    val singleThreadPool: ExecutorService by lazy {
        FinalizableExecutorService(ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS,
            LinkedTransferQueue<Runnable>()
        ) { r ->
            Thread(r, "firefly-single-thread-pool")
        })
    }

    val computation: CoroutineDispatcher by lazy {
        ForkJoinPool(defaultPoolSize, { pool ->
            val worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool)
            worker.name = "firefly-computation-pool-" + worker.poolIndex
            worker
        }, null, true).asCoroutineDispatcher()
    }
    val ioBlocking: CoroutineDispatcher by lazy { ioBlockingPool.asCoroutineDispatcher() }
    val singleThread: CoroutineDispatcher by lazy { singleThreadPool.asCoroutineDispatcher() }

    fun newSingleThreadExecutor(name: String): ExecutorService {
        val executor = ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS, LinkedTransferQueue<Runnable>()
        ) { r ->
            Thread(r, name)
        }
        return FinalizableExecutorService(executor)
    }

    fun newFixedThreadExecutor(name: String, poolSize: Int): ExecutorService {
        require(poolSize > 0)
        val executor = ThreadPoolExecutor(
            Math.min(defaultPoolSize, poolSize),
            Math.max(defaultPoolSize, poolSize),
            30,
            TimeUnit.SECONDS,
            LinkedTransferQueue<Runnable>()
        ) { r ->
            Thread(r, name)
        }
        return FinalizableExecutorService(executor)
    }

    fun newSingleThreadDispatcher(name: String): CoroutineDispatcher {
        return newSingleThreadExecutor(name).asCoroutineDispatcher()
    }

    fun newFixedThreadDispatcher(name: String, poolSize: Int): CoroutineDispatcher {
        return newFixedThreadExecutor(name, poolSize).asCoroutineDispatcher()
    }
}

class FinalizableExecutorService(private val executor: ExecutorService) : ExecutorService by executor {
    protected fun finalize() {
        ExecutorServiceUtils.shutdownAndAwaitTermination(executor, 5, TimeUnit.SECONDS)
    }
}