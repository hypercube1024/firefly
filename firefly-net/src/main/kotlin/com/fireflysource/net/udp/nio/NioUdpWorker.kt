package com.fireflysource.net.udp.nio

import com.fireflysource.common.coroutine.CoroutineDispatchers.newSingleThreadExecutor
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.SystemLogger
import java.nio.channels.Selector
import java.util.concurrent.TimeUnit

class NioUdpWorker(
    id: Int
) : AbstractLifeCycle() {

    companion object {
        private val log = SystemLogger.create(NioUdpWorker::class.java)
        private val timeUnit = TimeUnit.SECONDS
    }

    private val executor = newSingleThreadExecutor("firefly-nio-udp-worker-thread-$id")
    private val selector = Selector.open()

    override fun init() {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }

}