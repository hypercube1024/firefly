package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.CoroutineDispatchers
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Pengtao Qiu
 */
abstract class AbstractAioTcpChannelGroup : AbstractLifeCycle() {

    protected val id: AtomicInteger = AtomicInteger(0)
    protected val group: AsynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(
        ForkJoinPool(
            CoroutineDispatchers.defaultPoolSize, { pool ->
                val worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool)
                worker.name = "firefly-${getThreadName()}-" + worker.poolIndex
                worker
            }, null, true
                    )
                                                                                           )

    abstract fun getThreadName(): String

    override fun init() {
    }

    override fun destroy() {
        group.shutdown()
        try {
            // Wait a while for existing tasks to terminate
            if (!group.awaitTermination(10, TimeUnit.SECONDS)) {
                group.shutdownNow() // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!group.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("The tcp client channel group  did not terminate")
                }
            }
        } catch (ie: InterruptedException) {
            // (Re-)Cancel if current thread also interrupted
            group.shutdownNow()
            // Preserve interrupt status
            Thread.currentThread().interrupt()
        }
    }

}