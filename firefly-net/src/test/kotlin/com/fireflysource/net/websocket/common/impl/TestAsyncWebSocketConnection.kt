package com.fireflysource.net.websocket.common.impl

import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.tcp.TcpClientFactory
import com.fireflysource.net.tcp.TcpServerFactory
import com.fireflysource.net.tcp.onAcceptAsync
import com.fireflysource.net.websocket.common.frame.Frame
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
                arguments("none"),
                arguments("fragment"),
                arguments("identity"),
                arguments("deflate-frame"),
                arguments("permessage-deflate"),
                arguments("x-webkit-deflate-frame")
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
        server.onAcceptAsync { connection ->
            val policy = WebSocketPolicy(WebSocketBehavior.SERVER)
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
            val webSocketConnection = AsyncWebSocketConnection(
                connection,
                policy,
                upgradeRequest, upgradeResponse
            )
            webSocketConnection.setWebSocketMessageHandler { frame, c ->
                println("Server receive: ${frame.type}")
                when (frame.type) {
                    Frame.Type.TEXT -> {
                        val textFrame = frame as TextFrame
                        println("server receive: ${textFrame.payloadAsUTF8}")
                        c.sendText("response ${textFrame.payloadAsUTF8}")
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

        val policy = WebSocketPolicy(WebSocketBehavior.CLIENT)
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
        val webSocketConnection = AsyncWebSocketConnection(
            connection,
            policy,
            upgradeRequest, upgradeResponse
        )
        webSocketConnection.setWebSocketMessageHandler { frame, _ ->
            when (frame.type) {
                Frame.Type.TEXT -> {
                    val textFrame = frame as TextFrame
                    val str = textFrame.payloadAsUTF8
                    println("Client receive: $str")
                    channel.offer(str)
                    Result.DONE
                }
                else -> Result.DONE
            }
        }
        webSocketConnection.begin()

        (1..10).forEach {
            webSocketConnection.sendText("text: $it").await()
        }

        (1..10).forEach {
            val text = channel.receive()
            assertEquals("response text: $it", text)
        }

        webSocketConnection.closeFuture().await()

        client.stop()
        server.stop()
    }
}