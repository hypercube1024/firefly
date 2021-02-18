package com.fireflysource.net.tcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun TcpServer.onAcceptAsync(block: suspend CoroutineScope.(connection: TcpConnection) -> Unit): TcpServer {
    this.onAccept { connection -> connection.coroutineScope.launch { block.invoke(this, connection) } }
    return this
}