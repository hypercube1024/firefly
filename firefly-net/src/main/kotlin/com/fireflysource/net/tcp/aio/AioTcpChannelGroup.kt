package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.CoroutineDispatchers.awaitTerminationTimeout
import com.fireflysource.common.coroutine.CoroutineDispatchers.defaultPoolSize
import com.fireflysource.common.coroutine.CoroutineDispatchers.newSingleThreadDispatcher
import com.fireflysource.common.coroutine.CoroutineDispatchers.newSingleThreadExecutor
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.TcpChannelGroup
import kotlinx.coroutines.CoroutineDispatcher
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousChannelGroup.withThreadPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

class AioTcpChannelGroup(threadName: String) : AbstractLifeCycle(), TcpChannelGroup {

    companion object {
        private val log = SystemLogger.create(AioTcpChannelGroup::class.java)
    }

    private val id: AtomicInteger = AtomicInteger(0)
    private val group: AsynchronousChannelGroup =
        withThreadPool(newSingleThreadExecutor("firefly-aio-channel-group-thread"))
    private val dispatchers: Array<CoroutineDispatcher> = Array(defaultPoolSize) { i ->
        newSingleThreadDispatcher("firefly-$threadName-$i")
    }

    override fun getDispatcher(connectionId: Int): CoroutineDispatcher {
        return dispatchers[abs(connectionId % defaultPoolSize)]
    }

    override fun getAsynchronousChannelGroup(): AsynchronousChannelGroup = group

    override fun getNextId(): Int = id.getAndIncrement()

    override fun init() {
        log.info { "Initialize TCP channel group. boss: 1, worker: $defaultPoolSize" }
    }

    override fun destroy() {
        group.shutdown()
        try {
            // Wait a while for existing tasks to terminate
            if (!group.awaitTermination(awaitTerminationTimeout, TimeUnit.SECONDS)) {
                group.shutdownNow() // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!group.awaitTermination(awaitTerminationTimeout, TimeUnit.SECONDS)) {
                    log.info("The TCP channel group did not terminate")
                }
            }
        } catch (ie: InterruptedException) {
            // (Re-)Cancel if current thread also interrupted
            group.shutdownNow()
            // Preserve interrupt status
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            log.info { "shutdown channel group exception. ${e.message}" }
        }
    }

}