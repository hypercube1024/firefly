package com.fireflysource.net.websocket.common.impl

import com.fireflysource.common.sys.Result
import com.fireflysource.net.tcp.TcpClientFactory
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpServerFactory
import com.fireflysource.net.tcp.onAcceptAsync
import com.fireflysource.net.websocket.common.WebSocketConnection
import com.fireflysource.net.websocket.common.frame.TextFrame
import com.fireflysource.net.websocket.common.model.WebSocketBehavior
import com.fireflysource.net.websocket.common.model.WebSocketPolicy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.random.Random

/**
 * @author Pengtao Qiu
 */
class TestAsyncWebSocketConnection {

    companion object {
        @JvmStatic
        fun testParametersProvider(): Stream<Arguments> {
            return Stream.of(
                arguments("none", false),
                arguments("fragment", false),
                arguments("identity", false),
                arguments("deflate-frame", false),
                arguments("permessage-deflate", false),
                arguments("x-webkit-deflate-frame", false),
                arguments("identity,permessage-deflate", false),
                arguments("fragment,identity,permessage-deflate", false),

                arguments("none", true),
                arguments("fragment", true),
                arguments("identity", true),
                arguments("deflate-frame", true),
                arguments("permessage-deflate", true),
                arguments("x-webkit-deflate-frame", true),
                arguments("identity,permessage-deflate", true),
                arguments("fragment,identity,permessage-deflate", true)
            )
        }
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should send and receive websocket messages successfully.")
    fun test(extension: String, tls: Boolean) = runBlocking {
        val host = "localhost"
        val port = Random.nextInt(10000, 20000)

        val server = TcpServerFactory.create()
        if (tls) {
            server.enableSecureConnection()
        }
        server.onAcceptAsync { connection ->
            connection.beginHandshake().await()
            val webSocketConnection = createWebSocketConnection(WebSocketBehavior.SERVER, extension, connection)
            webSocketConnection.setWebSocketMessageHandler { frame, conn ->
                println("Server receive: ${frame.type}")
                when (frame) {
                    is TextFrame -> {
                        val payload = frame.payloadAsUTF8
                        println("server receive: $payload")
                        conn.sendText("response $payload")
                    }
                    else -> Result.DONE
                }
            }
            webSocketConnection.begin()
        }
        server.listen(host, port)

        val channel = Channel<String>(Channel.UNLIMITED)
        val client = TcpClientFactory.create()
        if (tls) {
            client.enableSecureConnection()
        }
        val connection = client.connect(host, port).await()
        connection.beginHandshake().await()
        val webSocketConnection = createWebSocketConnection(WebSocketBehavior.CLIENT, extension, connection)
        webSocketConnection.setWebSocketMessageHandler { frame, _ ->
            when (frame) {
                is TextFrame -> {
                    val payload = frame.payloadAsUTF8
                    println("Client receive: $payload")
                    channel.offer(payload)
                    Result.DONE
                }
                else -> Result.DONE
            }
        }
        webSocketConnection.begin()

        (1..10).forEach { webSocketConnection.sendText("text: $it") }

        (1..10).forEach {
            val text = channel.receive()
            assertEquals("response text: $it", text)
        }

        webSocketConnection.closeAsync()
        client.stop()
        server.stop()
    }

    private fun createWebSocketConnection(
        behavior: WebSocketBehavior,
        extension: String,
        connection: TcpConnection
    ): WebSocketConnection {
        val policy = WebSocketPolicy(behavior)
        val extensions = if (extension != "none") listOf(extension) else listOf()
        return AsyncWebSocketConnection(connection, policy, "localhost", extensions)
    }
}