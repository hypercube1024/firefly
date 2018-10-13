package com.firefly.kotlin.ext.websocket

import com.firefly.client.websocket.SimpleWebSocketClient
import com.firefly.codec.websocket.stream.WebSocketConnection
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.withTimeout
import java.util.concurrent.TimeUnit

/**
 * @author Pengtao Qiu
 */
suspend fun SimpleWebSocketClient.HandshakeBuilder.asyncConnect(
    time: Long = 10 * 1000L,
    unit: TimeUnit = TimeUnit.MILLISECONDS
                                                               ): WebSocketConnection =
    withTimeout(unit.toMillis(time)) { connect().await() }