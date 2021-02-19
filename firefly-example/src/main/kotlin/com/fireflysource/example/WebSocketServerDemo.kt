package com.fireflysource.example

import com.fireflysource.`$`
import com.fireflysource.common.io.useAwait
import com.fireflysource.common.sys.Result
import com.fireflysource.net.websocket.client.impl.connectAsync
import com.fireflysource.net.websocket.common.WebSocketConnection
import com.fireflysource.net.websocket.common.frame.Frame
import com.fireflysource.net.websocket.common.frame.TextFrame
import com.fireflysource.net.websocket.server.impl.onAcceptAsync
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.CompletableFuture

fun main() {
    `$`.httpServer().websocket("/websocket/hello")
        .onMessage { frame, _ -> onMessage(frame) }
        .onAcceptAsync { connection -> sendMessage("Server", connection) }
        .listen("localhost", 8090)

    val url = "ws://localhost:8090"
    `$`.httpClient().websocket("$url/websocket/hello")
        .extensions(listOf("permessage-deflate"))
        .onMessage { frame, _ -> onMessage(frame) }
        .connectAsync { connection -> sendMessage("Client", connection) }
}

private suspend fun sendMessage(content: String, connection: WebSocketConnection) = connection.useAwait {
    (1..10).forEach {
        connection.sendText("${content}. message: $it, time: ${Date()}")
        delay(1000)
    }
}

private fun onMessage(frame: Frame): CompletableFuture<Void> {
    if (frame is TextFrame) {
        println(frame.payloadAsUTF8)
    }
    return Result.DONE
}