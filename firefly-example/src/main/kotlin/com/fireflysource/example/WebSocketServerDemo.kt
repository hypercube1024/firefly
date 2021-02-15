package com.fireflysource.example

import com.fireflysource.`$`
import com.fireflysource.common.sys.Result
import com.fireflysource.net.websocket.client.impl.connectAsync
import com.fireflysource.net.websocket.common.frame.TextFrame
import com.fireflysource.net.websocket.server.impl.onAcceptAsync
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import java.util.*

fun main() {
    `$`.httpServer()
        .websocket("/websocket/hello")
        .onMessage { frame, _ ->
            if (frame is TextFrame) {
                println(frame.payloadAsUTF8)
            }
            Result.DONE
        }
        .onAcceptAsync { connection ->
            (1..10).forEach {
                connection.sendText("Server. message: $it, time: ${Date()}")
                delay(1000)
            }
            connection.closeAsync().await()
        }
        .listen("localhost", 8090)

    val url = "ws://localhost:8090"
    `$`.httpClient().websocket("$url/websocket/hello")
        .extensions(listOf("permessage-deflate"))
        .onMessage { frame, _ ->
            if (frame is TextFrame) {
                println(frame.payloadAsUTF8)
            }
            Result.DONE
        }
        .connectAsync { connection ->
            (1..10).forEach {
                connection.sendText("Client. message: $it, time: ${Date()}")
                delay(1000)
            }
            connection.closeAsync().await()
        }
}