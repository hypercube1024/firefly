package com.fireflysource.net.tcp.aio

import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import kotlinx.coroutines.*

class AioTcpCoroutineDispatcher(
    private val dispatcher: CoroutineDispatcher,
    private val supervisor: CompletableJob = SupervisorJob(),
    private val scope: CoroutineScope = CoroutineScope(dispatcher + supervisor)
) : TcpCoroutineDispatcher {
    
    override fun getCoroutineDispatcher(): CoroutineDispatcher = dispatcher

    override fun getSupervisorJob(): CompletableJob = supervisor

    override fun getCoroutineScope(): CoroutineScope = scope

    override fun execute(runnable: Runnable) {
        scope.launch { runnable.run() }
    }
}