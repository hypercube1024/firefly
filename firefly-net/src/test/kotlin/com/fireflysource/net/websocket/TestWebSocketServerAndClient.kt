package com.fireflysource.net.websocket

import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.server.HttpServerFactory
import com.fireflysource.net.websocket.common.frame.Frame
import com.fireflysource.net.websocket.common.frame.TextFrame
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
class TestWebSocketServerAndClient {

    companion object {
        @JvmStatic
        fun testParametersProvider(): Stream<Arguments> {
            return Stream.of(
                arguments("none", "ws"),
                arguments("fragment", "ws"),
                arguments("identity", "ws"),
                arguments("deflate-frame", "ws"),
                arguments("permessage-deflate", "ws"),
                arguments("x-webkit-deflate-frame", "ws"),
                arguments("identity,permessage-deflate", "ws"),
                arguments("fragment,identity,permessage-deflate", "ws"),

                arguments("none", "wss"),
                arguments("fragment", "wss"),
                arguments("identity", "wss"),
                arguments("deflate-frame", "wss"),
                arguments("permessage-deflate", "wss"),
                arguments("x-webkit-deflate-frame", "wss"),
                arguments("identity,permessage-deflate", "wss"),
                arguments("fragment,identity,permessage-deflate", "wss")
            )
        }
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should receive websocket messages successfully.")
    fun test(extensions: String, scheme: String) = runBlocking {
        val host = "localhost"
        val port = Random.nextInt(10000, 20000)
        val count = 100

        val serverChannel = Channel<String>(Channel.UNLIMITED)
        val server = HttpServerFactory.create()
        if (scheme == "wss") {
            server.enableSecureConnection()
        }
        server.websocket("/websocket/echo")
            .onMessage { frame, _ ->
                if (frame.type == Frame.Type.TEXT && frame is TextFrame) {
                    serverChannel.offer(frame.payloadAsUTF8)
                }
                Result.DONE
            }
            .onAccept { connection ->
                (1..count).forEach { i -> connection.sendText("Server $i") }
                Result.DONE
            }
            .listen(host, port)

        val clientChannel = Channel<String>(Channel.UNLIMITED)
        val client = HttpClientFactory.create()
        val webSocketConnection = client
            .websocket("$scheme://$host:$port/websocket/echo")
            .extensions(extensions.split(","))
            .onMessage { frame, _ ->
                if (frame.type == Frame.Type.TEXT && frame is TextFrame) {
                    clientChannel.offer(frame.payloadAsUTF8)
                }
                Result.DONE
            }
            .connect()
            .await()

        (1..count).forEach { i -> webSocketConnection.sendText("Client $i") }

        (1..count).forEach { i ->
            val serverReceivedMessage = serverChannel.receive()
            assertEquals("Client $i", serverReceivedMessage)
        }

        (1..count).forEach { i ->
            val clientReceivedMessage = clientChannel.receive()
            assertEquals("Server $i", clientReceivedMessage)
        }

        webSocketConnection.closeFuture()
        client.stop()
        server.stop()
    }
}