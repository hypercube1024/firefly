package com.fireflysource.net.tcp.aio

import com.fireflysource.common.lifecycle.AbstractLifeCycle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Pengtao Qiu
 */
abstract class AbstractAioTcpChannelGroup : AbstractLifeCycle() {

    protected val id: AtomicInteger = AtomicInteger(0)
    protected val group: AsynchronousChannelGroup =
        AsynchronousChannelGroup.withThreadPool(Executors.newSingleThreadExecutor {
            Thread(it, "firefly-${getThreadName()}")
        })
    protected val messageThread: CoroutineDispatcher =
        Executors.newSingleThreadExecutor {
            Thread(it, "firefly-${getThreadName()}-message-thread")
        }.asCoroutineDispatcher()

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