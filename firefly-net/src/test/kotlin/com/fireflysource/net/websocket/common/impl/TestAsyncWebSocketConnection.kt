package com.fireflysource.net.websocket.common.impl

import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.tcp.TcpClientFactory
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpServerFactory
import com.fireflysource.net.websocket.common.WebSocketConnection
import com.fireflysource.net.websocket.common.frame.DataFrame
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
                arguments("none"),
                arguments("fragment"),
                arguments("identity"),
                arguments("deflate-frame"),
                arguments("permessage-deflate"),
                arguments("x-webkit-deflate-frame"),
                arguments("identity,permessage-deflate"),
                arguments("fragment,identity,permessage-deflate")
            )
        }
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should send and receive websocket messages successfully.")
    fun test(extension: String) = runBlocking {
        val host = "localhost"
        val port = Random.nextInt(10000, 20000)

        val server = TcpServerFactory.create()
        server.onAccept { connection ->
            val webSocketConnection = createWebSocketConnection(WebSocketBehavior.SERVER, extension, connection)
            webSocketConnection.setWebSocketMessageHandler { frame, c ->
                println("Server receive: ${frame.type}")
                when (frame) {
                    is DataFrame -> {
                        val payload = frame.payloadAsUTF8
                        println("server receive: $payload")
                        c.sendText("response $payload")
                    }
                    else -> Result.DONE
                }
            }
            webSocketConnection.begin()
        }
        server.listen(host, port)

        val channel = Channel<String>(Channel.UNLIMITED)
        val client = TcpClientFactory.create()
        val connection = client.connect(host, port).await()
        val webSocketConnection = createWebSocketConnection(WebSocketBehavior.CLIENT, extension, connection)
        webSocketConnection.setWebSocketMessageHandler { frame, _ ->
            when (frame) {
                is DataFrame -> {
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

        webSocketConnection.closeFuture().await()
        client.stop()
        server.stop()
    }

    private fun createWebSocketConnection(
        behavior: WebSocketBehavior,
        extension: String,
        connection: TcpConnection
    ): WebSocketConnection {
        val policy = WebSocketPolicy(behavior)
        val uri = HttpURI()
        val fields = HttpFields()
        val upgradeRequest = MetaData.Request(HttpMethod.GET.value, uri, HttpVersion.HTTP_1_1, fields)
        if (extension != "none") {
            upgradeRequest.fields.put(HttpHeader.SEC_WEBSOCKET_EXTENSIONS, extension)
        }
        val upgradeResponse = MetaData.Response(HttpFields())
        if (extension != "none") {
            upgradeResponse.fields.put(HttpHeader.SEC_WEBSOCKET_EXTENSIONS, extension)
        }
        return AsyncWebSocketConnection(
            connection,
            policy,
            upgradeRequest, upgradeResponse
        )
    }
}