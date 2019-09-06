package com.fireflysource.net.tcp

import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.coroutine.launchGlobally
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.await

fun TcpConnection.launch(block: suspend CoroutineScope.() -> Unit): Job = launchGlobally(coroutineDispatcher, block)

fun <T> TcpConnection.async(block: suspend CoroutineScope.() -> T): Deferred<T> =
    asyncGlobally(coroutineDispatcher, block)

fun TcpConnection.launchWithAttr(
    attributes: MutableMap<String, Any>? = null,
    block: suspend CoroutineScope.() -> Unit
): Job = com.fireflysource.common.coroutine.launchWithAttr(coroutineDispatcher, attributes, block)

fun <T> TcpConnection.asyncWithAttr(
    attributes: MutableMap<String, Any>? = null,
    block: suspend CoroutineScope.() -> T
): Deferred<T> = com.fireflysource.common.coroutine.asyncWithAttr(coroutineDispatcher, attributes, block)

suspend fun TcpConnection.startReadingAndAwaitHandshake(): TcpConnection {
    this.startReading()
    if (this.isSecureConnection) {
        this.onHandshakeComplete().await()
    }
    return this
}

fun TcpServer.onAcceptAsync(block: suspend CoroutineScope.(connection: TcpConnection) -> Unit): TcpServer {
    this.onAccept { connection ->
        val job = connection.launch { block.invoke(this, connection) }
        connection.onClose { job.cancel() }
    }
    return this
}