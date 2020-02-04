package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.CoroutineDispatchers.awaitTerminationTimeout
import com.fireflysource.common.coroutine.CoroutineDispatchers.defaultPoolSize
import com.fireflysource.common.coroutine.CoroutineDispatchers.newSingleThreadDispatcher
import com.fireflysource.common.coroutine.CoroutineDispatchers.newSingleThreadExecutor
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.SystemLogger
import kotlinx.coroutines.CoroutineDispatcher
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/**
 * The asynchronous channel group and message thread group. It manages the IO and message threads.
 *
 * @author Pengtao Qiu
 */
abstract class AbstractAioTcpChannelGroup : AbstractLifeCycle() {

    companion object {
        private val log = SystemLogger.create(AbstractAioTcpChannelGroup::class.java)
    }

    protected val id: AtomicInteger = AtomicInteger(0)
    protected val group: AsynchronousChannelGroup =
        AsynchronousChannelGroup.withThreadPool(newSingleThreadExecutor("firefly-aio-channel-group-thread"))
    private val messageThreadGroup: Array<CoroutineDispatcher> = Array(defaultPoolSize) { i ->
        newSingleThreadDispatcher("firefly-${getThreadName()}-message-thread-$i")
    }

    protected fun getMessageThread(connectionId: Int): CoroutineDispatcher {
        return messageThreadGroup[abs(connectionId % defaultPoolSize)]
    }

    abstract fun getThreadName(): String

    override fun init() {
        log.info { "initialize single thread asynchronous channel group." }
        log.info { "initialize message thread group. thread number: $defaultPoolSize" }
    }

    override fun destroy() {
        group.shutdown()
        try {
            // Wait a while for existing tasks to terminate
            if (!group.awaitTermination(awaitTerminationTimeout, TimeUnit.SECONDS)) {
                group.shutdownNow() // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!group.awaitTermination(awaitTerminationTimeout, TimeUnit.SECONDS)) {
                    System.err.println("The tcp client channel group did not terminate")
                }
            }
        } catch (ie: InterruptedException) {
            // (Re-)Cancel if current thread also interrupted
            group.shutdownNow()
            // Preserve interrupt status
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            log.info { "shutdown channel group exception." }
        }
    }

}