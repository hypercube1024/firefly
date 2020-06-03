package com.fireflysource.net.websocket.common.impl

import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.common.coroutine.pollAll
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.discard
import com.fireflysource.common.sys.Result.futureToConsumer
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.Connection
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import com.fireflysource.net.websocket.common.WebSocketConnection
import com.fireflysource.net.websocket.common.WebSocketMessageHandler
import com.fireflysource.net.websocket.common.decoder.Parser
import com.fireflysource.net.websocket.common.encoder.Generator
import com.fireflysource.net.websocket.common.exception.NextIncomingFramesNotSetException
import com.fireflysource.net.websocket.common.frame.*
import com.fireflysource.net.websocket.common.model.*
import com.fireflysource.net.websocket.common.stream.ConnectionState
import com.fireflysource.net.websocket.common.stream.ExtensionNegotiator
import com.fireflysource.net.websocket.common.stream.IOState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.CancellationException
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
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, WebSocketConnection,
    IncomingFrames, OutgoingFrames {

    companion object {
        private val log = SystemLogger.create(AsyncWebSocketConnection::class.java)
    }

    private val extensionNegotiator = ExtensionNegotiator()
    private val ioState = IOState()
    private val parser = Parser(webSocketPolicy)
    private val generator = Generator(webSocketPolicy)
    private val messageChannel = Channel<Frame>(Channel.UNLIMITED)
    private var messageHandler: WebSocketMessageHandler? = null

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
        return sendFrame(binaryFrame)
    }

    override fun sendText(text: String): CompletableFuture<Void> {
        val textFrame = TextFrame()
        textFrame.setPayload(text)
        return sendFrame(textFrame)
    }

    override fun sendFrame(frame: Frame): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        outgoingFrame(frame, futureToConsumer(future))
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
            }
            else -> {
            }
        }
        extensionNegotiator.incomingFrames.incomingFrame(frame)
    }

    override fun setWebSocketMessageHandler(handler: WebSocketMessageHandler) {
        extensionNegotiator.setNextIncomingFrames { messageChannel.offer(it) }
        messageHandler = handler
    }

    override fun outgoingFrame(frame: Frame, result: Consumer<Result<Void>>) {
        extensionNegotiator.outgoingFrames.outgoingFrame(frame, result)
    }

    override fun getUpgradeRequest(): MetaData.Request = upgradeRequest

    override fun getUpgradeResponse(): MetaData.Response = upgradeResponse

    private fun close(code: Int, reason: String?): CompletableFuture<Void> {
        val closeInfo = CloseInfo(code, reason)
        return close(closeInfo)
    }

    private fun close(closeInfo: CloseInfo): CompletableFuture<Void> {
        val closeFrame = closeInfo.asFrame()
        return sendFrame(closeFrame)
    }

    override fun closeFuture(): CompletableFuture<Void> {
        return close(StatusCode.NORMAL, null)
    }

    override fun close() {
        closeFuture()
    }

    override fun begin() {
        if (extensionNegotiator.nextIncomingFrames == null) {
            throw NextIncomingFramesNotSetException("Please set the next incoming frames listener before start websocket connection.")
        }

        parser.incomingFramesHandler = this
        setNextOutgoingFrames()
        configureFromExtensions()
        ioState.onConnected()

        receiveMessageJob()
        parseFrameJob()
        ioState.addListener { state ->
            when (state) {
                ConnectionState.CLOSED -> tcpConnection.closeFuture()
                ConnectionState.CLOSING -> {
                    if (ioState.isOutputAvailable && ioState.isRemoteCloseInitiated) {
                        close(StatusCode.NORMAL, null)
                    }
                }
                else -> {
                }
            }
        }
        ioState.onOpened()
    }

    private fun parseFrameJob() {
        tcpConnection.coroutineScope.launch {
            while (true) {
                try {
                    val buffer = tcpConnection.read().await()
                    parser.parse(buffer)
                } catch (e: CancellationException) {
                    log.info { "The websocket parsing job canceled. id: ${this@AsyncWebSocketConnection.id}" }
                    break
                } catch (e: Exception) {
                    log.error(e) { "Parse websocket frame error. id: ${this@AsyncWebSocketConnection.id}" }
                    ioState.onReadFailure(e)
                    break
                }
            }
        }
    }

    private fun receiveMessageJob() {
        tcpConnection.coroutineScope.launch {
            while (true) {
                val frame = messageChannel.receive()
                try {
                    messageHandler?.handle(frame, this@AsyncWebSocketConnection)?.await()
                } catch (e: CancellationException) {
                    log.info { "The websocket receiving message job canceled. id: ${this@AsyncWebSocketConnection.id}" }
                } catch (e: Exception) {
                    log.error(e) { "Handle websocket frame exception. id: ${this@AsyncWebSocketConnection.id}" }
                }
            }
        }.invokeOnCompletion { cause ->
            log.info { "The websocket connection closed, handle the remaining message. id: ${this@AsyncWebSocketConnection.id},  cause: ${cause?.message}" }
            messageChannel.pollAll { frame ->
                try {
                    messageHandler?.handle(frame, this@AsyncWebSocketConnection)
                } catch (e: Exception) {
                    log.error(e) { "Handle websocket frame exception. id: ${this@AsyncWebSocketConnection.id}" }
                }
            }
        }
    }

    private fun configureFromExtensions() {
        val fields = upgradeResponse.fields
        extensionNegotiator.configureExtensions(fields, parser, generator, policy)
    }

    private fun setNextOutgoingFrames() {
        extensionNegotiator.setNextOutgoingFrames { frame, result ->
            if (policy.behavior == WebSocketBehavior.CLIENT && frame is WebSocketFrame && !frame.isMasked) {
                frame.mask = generateMask()
            }

            val buf = BufferUtils.allocate(Generator.MAX_HEADER_LENGTH + frame.payloadLength)
            val pos = buf.flipToFill()
            generator.generateWholeFrame(frame, buf)
            buf.flipToFlush(pos)
            tcpConnection.write(buf)
                .thenCompose { tcpConnection.flush() }
                .thenAccept {
                    if (frame.type == Frame.Type.CLOSE && frame is CloseFrame) {
                        val closeInfo = CloseInfo(frame.getPayload(), false)
                        ioState.onCloseLocal(closeInfo)
                    }
                    result.accept(Result.SUCCESS)
                }
                .exceptionallyAccept {
                    result.accept(Result.createFailedResult(it))
                    ioState.onWriteFailure(it)
                }
        }
    }

}