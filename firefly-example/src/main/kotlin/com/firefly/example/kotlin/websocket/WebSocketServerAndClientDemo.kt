package com.firefly.example.kotlin.websocket

import com.firefly.kotlin.ext.common.firefly

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) {
    val server = firefly.createWebSocketServer()
    server.webSocket("/helloWebSocket")
            .onConnect { conn -> conn.sendText("OK.") }
            .onText { text, _ -> println("The server received: $text") }
            .listen("localhost", 8080)

    val client = firefly.createWebSocketClient()
    client.webSocket("ws://localhost:8080/helloWebSocket")
            .onText { text, _ -> println("The client received: $text") }
            .connect()
            .thenAccept { conn -> conn.sendText("Hello server.") }
}