package com.fireflysource.net.udp.nio

import com.fireflysource.net.udp.UdpCoroutineDispatcher
import kotlinx.coroutines.*

class NioUdpCoroutineDispatcher(
    id: Int,
    private val dispatcher: CoroutineDispatcher,
    private val supervisor: CompletableJob = SupervisorJob(),
    private val scope: CoroutineScope = CoroutineScope(dispatcher + supervisor + CoroutineName("UdpConnection#$id"))
) : UdpCoroutineDispatcher {
    override fun execute(runnable: Runnable) {
        scope.launch { runnable.run() }
    }

    override fun getCoroutineDispatcher(): CoroutineDispatcher = dispatcher

    override fun getCoroutineScope(): CoroutineScope = scope

    override fun getSupervisorJob(): CompletableJob = supervisor
}