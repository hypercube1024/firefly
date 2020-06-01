package com.fireflysource.net.websocket.common.impl

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.discard
import com.fireflysource.common.sys.Result.futureToConsumer
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.Connection
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import com.fireflysource.net.websocket.common.WebSocketConnection
import com.fireflysource.net.websocket.common.decoder.Parser
import com.fireflysource.net.websocket.common.encoder.Generator
import com.fireflysource.net.websocket.common.exception.NextIncomingFramesNotSetException
import com.fireflysource.net.websocket.common.extension.AbstractExtension
import com.fireflysource.net.websocket.common.frame.*
import com.fireflysource.net.websocket.common.model.CloseInfo
import com.fireflysource.net.websocket.common.model.IncomingFrames
import com.fireflysource.net.websocket.common.model.WebSocketBehavior
import com.fireflysource.net.websocket.common.model.WebSocketPolicy
import com.fireflysource.net.websocket.common.stream.ExtensionNegotiator
import com.fireflysource.net.websocket.common.stream.IOState
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadLocalRandom
import java.util.function.Consumer


/**
 * @author Pengtao Qiu
 */
class AsyncWebSocketConnection(
    private val tcpConnection: TcpConnection,
    private val webSocketPolicy: WebSocketPolicy,
    private val upgradeRequest: MetaData.Request,
    private val upgradeResponse: MetaData.Response
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, WebSocketConnection, IncomingFrames {

    companion object {
        private val log = SystemLogger.create(AsyncWebSocketConnection::class.java)
    }

    private val extensionNegotiator = ExtensionNegotiator()
    private val ioState = IOState()
    private val parser = Parser(webSocketPolicy)
    private val generator = Generator(webSocketPolicy)

    override fun getPolicy(): WebSocketPolicy = webSocketPolicy

    override fun getIOState(): IOState = ioState

    override fun generateMask(): ByteArray {
        val mask = ByteArray(4)
        ThreadLocalRandom.current().nextBytes(mask)
        return mask
    }

    override fun sendData(data: ByteBuffer): CompletableFuture<Void> {
        val binaryFrame = BinaryFrame()
        binaryFrame.payload = data
        val future = CompletableFuture<Void>()
        outgoingFrame(binaryFrame, futureToConsumer(future))
        return future
    }

    override fun sendText(text: String): CompletableFuture<Void> {
        val textFrame = TextFrame()
        textFrame.setPayload(text)
        val future = CompletableFuture<Void>()
        outgoingFrame(textFrame, futureToConsumer(future))
        return future
    }

    override fun incomingFrame(frame: Frame) {
        when (frame.type) {
            Frame.Type.PING -> {
                val pong = PongFrame()
                outgoingFrame(pong, discard())
            }
            Frame.Type.PONG -> log.info { "The websocket connection received pong frame. id: ${this.id}" }
            Frame.Type.CLOSE -> {
                val closeFrame = frame as CloseFrame
                val closeInfo = CloseInfo(closeFrame.payload, false)
                ioState.onCloseRemote(closeInfo)
                tcpConnection.closeFuture()
            }
            else -> {
            }
        }
        extensionNegotiator.incomingFrames?.incomingFrame(frame)
    }

    override fun setNextIncomingFrames(nextIncomingFrames: IncomingFrames) {
        extensionNegotiator.nextIncomingFrames = nextIncomingFrames
    }

    override fun outgoingFrame(frame: Frame, result: Consumer<Result<Void>>) {
        extensionNegotiator.outgoingFrames.outgoingFrame(frame, result)
    }

    override fun getUpgradeRequest(): MetaData.Request = upgradeRequest

    override fun getUpgradeResponse(): MetaData.Response = upgradeResponse

    override fun closeFuture(): CompletableFuture<Void> = sendFrame(CloseFrame())

    override fun close() {
        closeFuture()
    }

    override fun begin() {
        if (extensionNegotiator.nextIncomingFrames == null) {
            throw NextIncomingFramesNotSetException("Please set the next incoming frames listener before start websocket connection.")
        }

        parser.incomingFramesHandler = this
        ioState.onOpened()

        extensionNegotiator.setNextOutgoingFrames { frame, result ->
            if (policy.behavior == WebSocketBehavior.CLIENT && frame is WebSocketFrame) {
                if (!frame.isMasked) {
                    frame.mask = generateMask()
                }
            }

            val buf = ByteBuffer.allocate(Generator.MAX_HEADER_LENGTH + frame.payloadLength)
            generator.generateWholeFrame(frame, buf)
            BufferUtils.flipToFlush(buf, 0)
            tcpConnection.write(buf) {
                if (it.isSuccess) result.accept(Result.SUCCESS)
                else result.accept(Result.createFailedResult(it.throwable))
            }

            if (frame.type == Frame.Type.CLOSE && frame is CloseFrame) {
                val closeInfo = CloseInfo(frame.getPayload(), false)
                getIOState().onCloseLocal(closeInfo)
                tcpConnection.closeFuture()
            }
        }

        val metaData =
            if (upgradeResponse.fields.contains(HttpHeader.SEC_WEBSOCKET_EXTENSIONS)) upgradeResponse
            else upgradeRequest

        val extensions = extensionNegotiator.createExtensionChain(metaData.fields)
        if (extensions.isNotEmpty()) {
            generator.configureFromExtensions(extensions)
            parser.configureFromExtensions(extensions)
            extensions.forEach {
                if (it is AbstractExtension) {
                    it.policy = policy
                }
            }
        }

        tcpConnection.coroutineScope.launch {
            while (true) {
                try {
                    val buffer = tcpConnection.read().await()
                    parser.parse(buffer)
                } catch (e: Exception) {
                    log.error(e) { "WebSocket frame parsing error." }
                    tcpConnection.closeFuture()
                    break
                }
            }
        }
    }
}