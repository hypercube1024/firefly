package com.fireflysource.net.tcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch

suspend fun TcpConnection.startReadingAndAwaitHandshake(): TcpConnection {
    this.startReading()
    if (this.isSecureConnection) {
        this.beginHandshake().await()
    }
    return this
}

fun TcpServer.onAcceptAsync(block: suspend CoroutineScope.(connection: TcpConnection) -> Unit): TcpServer {
    this.onAccept { connection -> connection.coroutineScope.launch { block.invoke(this, connection) } }
    return this
}