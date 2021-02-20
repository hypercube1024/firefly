package com.fireflysource.example

import com.fireflysource.`$`
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.BufferUtils.toBuffer
import com.fireflysource.common.io.useAwait
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.connectAsync
import com.fireflysource.net.tcp.aio.onAcceptAsync
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.*

fun main() {
    `$`.tcpServer().onAcceptAsync { connection ->
        launch { writeLoop("Server", connection) }
        launch { readLoop(connection) }
    }.listen("localhost", 8090)

    `$`.tcpClient().connectAsync("localhost", 8090) { connection ->
        launch { writeLoop("Client", connection) }
        launch { readLoop(connection) }
    }
}

private suspend fun readLoop(connection: TcpConnection) = connection.useAwait {
    while (true) {
        try {
            val buffer = connection.read().await()
            println(BufferUtils.toString(buffer))
        } catch (e: Exception) {
            println("Connection closed.")
            break
        }
    }
}

private suspend fun writeLoop(data: String, connection: TcpConnection) = connection.useAwait {
    (1..10).forEach {
        connection.write(toBuffer("TCP ${data}. count: $it, time: ${Date()}"))
        delay(1000)
    }
}