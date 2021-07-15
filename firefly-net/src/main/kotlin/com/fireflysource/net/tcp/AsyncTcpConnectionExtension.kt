package com.fireflysource.net.tcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.nio.ByteBuffer

fun TcpServer.onAcceptAsync(block: suspend CoroutineScope.(connection: TcpConnection) -> Unit): TcpServer {
    this.onAccept { connection -> connection.coroutineScope.launch { block.invoke(this, connection) } }
    return this
}

suspend fun TcpConnection.read(timeout: Long): ByteBuffer = withTimeout(timeout) {
    read().await()
}

suspend fun TcpConnection.close(timeout: Long): Unit = withTimeout(timeout) {
    closeAsync().await()
}

suspend fun TcpConnection.write(byteBuffer: ByteBuffer, timeout: Long): Int = withTimeout(timeout) {
    write(byteBuffer).await()
}

suspend fun TcpConnection.flush(timeout: Long): Unit = withTimeout(timeout) {
    flush().await()
}
