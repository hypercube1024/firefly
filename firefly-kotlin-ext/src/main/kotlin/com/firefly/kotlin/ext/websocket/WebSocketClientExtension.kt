package com.firefly.kotlin.ext.websocket

import com.firefly.client.websocket.SimpleWebSocketClient
import com.firefly.codec.websocket.stream.WebSocketConnection
import kotlinx.coroutines.experimental.future.await

/**
 * @author Pengtao Qiu
 */
suspend fun SimpleWebSocketClient.HandshakeBuilder.asyncConnect(): WebSocketConnection = connect().await()