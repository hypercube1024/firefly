package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.common.concurrent.AtomicBiInteger
import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.math.MathUtils
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.*
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.Connection
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.TcpBasedHttpConnection
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.v2.decoder.Parser
import com.fireflysource.net.http.common.v2.encoder.Generator
import com.fireflysource.net.http.common.v2.frame.*
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import com.fireflysource.net.tcp.aio.ChannelClosedException
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlin.math.min

abstract class AsyncHttp2Connection(
    initStreamId: Int,
    config: HttpConfig,
    private val tcpConnection: TcpConnection,
    private val flowControl: FlowControl,
    private val listener: Http2Connection.Listener
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, Http2Connection, TcpBasedHttpConnection,
    Parser.Listener {

    companion object {
        private val log = SystemLogger.create(AsyncHttp2Connection::class.java)
    }

    private val localStreamId = AtomicInteger(initStreamId)
    private val http2StreamMap = ConcurrentHashMap<Int, Stream>()
    private val localStreamCount = AtomicInteger()
    private val remoteStreamCount = AtomicBiInteger()
    private val lastRemoteStreamId = AtomicInteger()
    private val closeState = AtomicReference(CloseState.NOT_CLOSED)

    private val sendWindow = AtomicInteger(HttpConfig.DEFAULT_WINDOW_SIZE)
    private val recvWindow = AtomicInteger(HttpConfig.DEFAULT_WINDOW_SIZE)

    private val generator = Generator(config.maxDynamicTableSize, config.maxHeaderBlockFragment)

    private var maxLocalStreams: Int = -1
    private var maxRemoteStreams: Int = -1
    protected var initialSessionRecvWindow: Int = config.initialSessionRecvWindow
    private var pushEnabled: Boolean = false
    private var closeFrame: GoAwayFrame? = null

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_2

    override fun getTcpConnection(): TcpConnection = tcpConnection

    override fun newStream(headersFrame: HeadersFrame, promise: Consumer<Result<Stream?>>, listener: Stream.Listener) {
        val frameStreamId = headersFrame.streamId
        if (frameStreamId <= 0) {
            val nextStreamId = getNextStreamId()
            val priority = if (headersFrame.priority == null) {
                null
            } else {
                PriorityFrame(
                    nextStreamId,
                    headersFrame.priority.parentStreamId,
                    headersFrame.priority.weight,
                    headersFrame.priority.isExclusive
                )
            }
            val newHeadersFrame = HeadersFrame(nextStreamId, headersFrame.metaData, priority, headersFrame.isEndStream)
            val stream = createLocalStream(nextStreamId, listener)
            sendNewHeadersFrame(stream, newHeadersFrame, promise)

        } else {
            val stream = createLocalStream(frameStreamId, listener)
            sendNewHeadersFrame(stream, headersFrame, promise)
        }
    }

    private fun sendNewHeadersFrame(stream: Stream, newHeadersFrame: HeadersFrame, promise: Consumer<Result<Stream?>>) {
        sendControlFrame(stream, newHeadersFrame)
            .thenAccept { promise.accept(Result(true, stream, null)) }
            .exceptionally {
                promise.accept(createFailedResult(null, it))
                null
            }
    }

    fun removeStream(stream: Stream) {
        val removed = http2StreamMap.remove(stream.id)
        if (removed != null) {
            listener.onStreamClosed(this, stream)
            flowControl.onStreamDestroyed(stream)
            log.debug { "Removed $stream" }
        }
    }

    private fun getNextStreamId(): Int = localStreamId.getAndAdd(2)

    private fun createLocalStream(id: Int, listener: Stream.Listener): Stream {
        return http2StreamMap.computeIfAbsent(id) {
            checkMaxLocalStreams()
            val stream = AsyncHttp2Stream(this, id, true, listener)
            flowControl.onStreamCreated(stream)
            stream
        }
    }

    private fun checkMaxLocalStreams() {
        while (true) {
            val localCount = localStreamCount.get()
            val maxCount = maxLocalStreams
            check(localCount in 0..maxCount) { "Max local stream count $localCount exceeded $maxCount" }
            if (localStreamCount.compareAndSet(localCount, localCount + 1)) {
                break
            }
        }
    }

    fun sendControlFrame(stream: Stream?, vararg frames: Frame): CompletableFuture<Long> =
        asyncGlobally(tcpConnection.coroutineDispatcher) {
            var writeBytes = 0L

            frameLoop@ for (frame in frames) {
                val byteBuffers = generator.control(frame).byteBuffers

                when (frame.type) {
                    FrameType.HEADERS -> {
                        val headersFrame = frame as HeadersFrame
                        if (stream != null && stream is AsyncHttp2Stream) {
                            stream.updateClose(headersFrame.isEndStream, CloseState.Event.BEFORE_SEND)
                        }
                    }
                    FrameType.SETTINGS -> {
                        val settingsFrame = frame as SettingsFrame
                        val initialWindow = settingsFrame.settings[SettingsFrame.INITIAL_WINDOW_SIZE]
                        if (initialWindow != null) {
                            flowControl.updateInitialStreamWindow(this@AsyncHttp2Connection, initialWindow, true)
                        }
                    }
                    FrameType.DISCONNECT -> {
                        terminate()
                        break@frameLoop
                    }
                    else -> {
                        // ignore the other control frame types
                    }
                }

                try {
                    val bytes = tcpConnection.write(byteBuffers, 0, byteBuffers.size).await()
                    writeBytes += bytes

                    when (frame.type) {
                        FrameType.HEADERS -> {
                            val headersFrame = frame as HeadersFrame
                            if (stream != null && stream is AsyncHttp2Stream) {
                                listener.onStreamOpened(this@AsyncHttp2Connection, stream)
                                if (stream.updateClose(headersFrame.isEndStream, CloseState.Event.AFTER_SEND)) {
                                    removeStream(stream)
                                }
                            }
                        }
                        FrameType.RST_STREAM -> {
                            if (stream != null && stream is AsyncHttp2Stream) {
                                stream.close()
                                removeStream(stream)
                            }
                        }
                        FrameType.PUSH_PROMISE -> {
                            if (stream != null && stream is AsyncHttp2Stream) {
                                stream.updateClose(true, CloseState.Event.RECEIVED)
                            }
                        }
                        FrameType.GO_AWAY -> {
                            val future = CompletableFuture<Void>()
                            tcpConnection.close(futureToConsumer(future))
                            future.await()
                            log.info { "Send go away frame and close TCP connection success" }
                        }
                        FrameType.WINDOW_UPDATE -> {
                            flowControl.windowUpdate(this@AsyncHttp2Connection, stream, frame as WindowUpdateFrame)
                        }
                        else -> {
                            // ignore the other control frame types
                        }
                    }
                } catch (e: ChannelClosedException) {
                    log.warn {
                        val connectionInfo = this@AsyncHttp2Connection.toString()
                        "The socket channel has been closed. $connectionInfo "
                    }
                    throw e
                }
            }
            writeBytes
        }.asCompletableFuture()

    private fun terminate() {
        terminateLoop@ while (true) {
            when (val current = closeState.get()) {
                CloseState.NOT_CLOSED, CloseState.LOCALLY_CLOSED, CloseState.REMOTELY_CLOSED -> {
                    if (closeState.compareAndSet(current, CloseState.CLOSED)) {
                        for (stream in http2StreamMap.values) {
                            (stream as AsyncHttp2Stream).close()
                        }
                        streams.clear()
                        disconnect()
                        break@terminateLoop
                    }
                }
                else -> {
                    // ignore the other close states
                    break@terminateLoop
                }
            }
        }
    }

    private fun disconnect() {
        log.debug { "Disconnecting $this" }
        tcpConnection.close()
    }

    override fun priority(frame: PriorityFrame, result: Consumer<Result<Void>>): Int {
        val stream = http2StreamMap[frame.streamId]
        if (stream == null) {
            val newStreamId = getNextStreamId()
            val newFrame = PriorityFrame(newStreamId, frame.parentStreamId, frame.weight, frame.isExclusive)
            sendControlFrame(null, newFrame)
                .thenAccept { result.accept(Result.SUCCESS) }
                .exceptionally {
                    result.accept(createFailedResult(it))
                    null
                }
            return newStreamId
        } else {
            sendControlFrame(stream, frame)
                .thenAccept { result.accept(Result.SUCCESS) }
                .exceptionally {
                    result.accept(createFailedResult(it))
                    null
                }
            return stream.id
        }
    }

    override fun settings(frame: SettingsFrame, result: Consumer<Result<Void>>) {
        sendControlFrame(null, frame)
            .thenAccept { result.accept(Result.SUCCESS) }
            .exceptionally {
                result.accept(createFailedResult(it))
                null
            }
    }

    override fun ping(frame: PingFrame, result: Consumer<Result<Void>>) {
        if (frame.isReply) {
            result.accept(createFailedResult(IllegalArgumentException("The reply must be false")))
        } else {
            sendControlFrame(null, frame)
                .thenAccept { result.accept(Result.SUCCESS) }
                .exceptionally {
                    result.accept(createFailedResult(it))
                    null
                }
        }
    }

    override fun getStreams(): MutableCollection<Stream> = http2StreamMap.values

    override fun getStream(streamId: Int): Stream? = http2StreamMap[streamId]

    override fun close(error: Int, reason: String, result: Consumer<Result<Void>>): Boolean {
        while (true) {
            when (val current: CloseState = closeState.get()) {
                CloseState.NOT_CLOSED -> {
                    if (closeState.compareAndSet(current, CloseState.LOCALLY_CLOSED)) {
                        val goAwayFrame = newGoAwayFrame(CloseState.LOCALLY_CLOSED, error, reason)
                        closeFrame = goAwayFrame
                        sendControlFrame(null, goAwayFrame)
                            .thenAccept { result.accept(Result.SUCCESS) }
                            .exceptionally {
                                result.accept(createFailedResult(it))
                                null
                            }
                        return true
                    }
                }
                else -> {
                    log.debug { "Ignoring close $error/$reason, already closed" }
                    result.accept(Result.SUCCESS)
                    return false
                }
            }
        }
    }

    override fun onData(frame: DataFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    abstract override fun onHeaders(frame: HeadersFrame)

    override fun onPriority(frame: PriorityFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onReset(frame: ResetFrame) {
        log.debug { "Received $frame" }
        val stream = getStream(frame.streamId)
        if (stream != null) {
            if (stream is AsyncHttp2Stream) {
                stream.process(frame, discard())
            }
        } else {
            onResetForUnknownStream(frame)
        }
    }

    protected abstract fun onResetForUnknownStream(frame: ResetFrame)

    protected fun reset(frame: ResetFrame, result: Consumer<Result<Void>>) {
        sendControlFrame(getStream(frame.streamId), frame)
            .thenAccept { result.accept(Result.SUCCESS) }
            .exceptionally {
                result.accept(createFailedResult(it))
                null
            }
    }

    override fun onSettings(frame: SettingsFrame) {
        // SPEC: SETTINGS frame MUST be replied.
        onSettings(frame, true)
    }

    fun onSettings(frame: SettingsFrame, reply: Boolean) {
        log.debug { "received frame: $frame" }
        if (frame.isReply) {
            return
        }

        frame.settings.forEach { (key, value) ->
            when (key) {
                SettingsFrame.HEADER_TABLE_SIZE -> {
                    log.debug { "Updating HPACK header table size to $value for $this" }
                    generator.setHeaderTableSize(value)
                }
                SettingsFrame.ENABLE_PUSH -> {
                    val enabled = value == 1
                    log.debug { "${if (enabled) "Enabling" else "Disabling"} push for $this" }
                    pushEnabled = enabled
                }
                SettingsFrame.MAX_CONCURRENT_STREAMS -> {
                    log.debug { "Updating max local concurrent streams to $value for $this" }
                    maxLocalStreams = value
                }
                SettingsFrame.INITIAL_WINDOW_SIZE -> {
                    log.debug { "Updating initial window size to $value for $this" }
                    flowControl.updateInitialStreamWindow(this, value, false)
                }
                SettingsFrame.MAX_FRAME_SIZE -> {
                    log.debug { "Updating max frame size to $value for $this" }
                    generator.setMaxFrameSize(value)
                }
                SettingsFrame.MAX_HEADER_LIST_SIZE -> {
                    log.debug { "Updating max header list size to $value for $this" }
                    generator.setMaxHeaderListSize(value)
                }
                else -> {
                    log.debug { "Unknown setting $key:$value for $this" }
                }
            }
        }

        notifySettings(this, frame)

        if (reply) {
            val replyFrame = SettingsFrame(emptyMap(), true)
            settings(replyFrame, discard())
        }
    }

    protected fun notifySettings(connection: Http2Connection, frame: SettingsFrame) {
        try {
            listener.onSettings(connection, frame)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
        }
    }

    override fun onPushPromise(frame: PushPromiseFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPing(frame: PingFrame) {
        if (frame.isReply) {
            notifyPing(this, frame)
        } else {
            val replay = PingFrame(frame.payload, true)
            sendControlFrame(null, replay)
        }
    }

    protected fun notifyPing(connection: Http2Connection, frame: PingFrame) {
        try {
            listener.onPing(connection, frame)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
        }
    }

    override fun onGoAway(frame: GoAwayFrame) {
        log.debug { "Received $frame" }
        closeLoop@ while (true) {
            when (val current: CloseState = closeState.get()) {
                CloseState.NOT_CLOSED -> {
                    if (closeState.compareAndSet(current, CloseState.REMOTELY_CLOSED)) {
                        // We received a GO_AWAY, so try to write what's in the queue and then disconnect.
                        closeFrame = frame
                        notifyClose(this, frame, Consumer {
                            val goAwayFrame = newGoAwayFrame(CloseState.CLOSED, ErrorCode.NO_ERROR.code, null)
                            val disconnectFrame = DisconnectFrame()
                            sendControlFrame(null, goAwayFrame, disconnectFrame)
                        })
                        break@closeLoop
                    }
                }
                else -> {
                    log.debug { "Ignored $frame, already closed" }
                    break@closeLoop
                }
            }
        }
    }

    protected fun notifyClose(connection: Http2Connection, frame: GoAwayFrame, consumer: Consumer<Result<Void>>) {
        try {
            listener.onClose(connection, frame, consumer)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
        }
    }

    private fun newGoAwayFrame(closeState: CloseState, error: Int, reason: String?): GoAwayFrame {
        var payload: ByteArray? = null
        if (reason != null) { // Trim the reason to avoid attack vectors.
            payload = reason.substring(0, min(reason.length, 32)).toByteArray(StandardCharsets.UTF_8)
        }
        return GoAwayFrame(closeState, getLastRemoteStreamId(), error, payload)
    }

    protected fun getLastRemoteStreamId(): Int {
        return lastRemoteStreamId.get()
    }

    override fun onWindowUpdate(frame: WindowUpdateFrame) {
        log.debug { "Received $frame" }
        val streamId = frame.streamId
        val windowDelta = frame.windowDelta
        if (streamId > 0) {
            val stream = getStream(streamId)
            if (stream != null && stream is AsyncHttp2Stream) {
                val streamSendWindow: Int = stream.updateSendWindow(0)
                if (MathUtils.sumOverflows(streamSendWindow, windowDelta)) {
                    reset(ResetFrame(streamId, ErrorCode.FLOW_CONTROL_ERROR.code), discard())
                } else {
                    stream.process(frame, discard())
                    onWindowUpdate(stream, frame)
                }
            } else {
                if (!isRemoteStreamClosed(streamId)) {
                    onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "unexpected_window_update_frame")
                }
            }
        } else {
            val sessionSendWindow = updateSendWindow(0)
            if (MathUtils.sumOverflows(sessionSendWindow, windowDelta)) {
                onConnectionFailure(ErrorCode.FLOW_CONTROL_ERROR.code, "invalid_flow_control_window")
            } else {
                onWindowUpdate(null, frame)
            }
        }
    }

    fun onWindowUpdate(stream: Stream?, frame: WindowUpdateFrame) {
        launchGlobally(tcpConnection.coroutineDispatcher) {
            flowControl.windowUpdate(this@AsyncHttp2Connection, stream, frame)
        }
    }

    protected fun isLocalStreamClosed(streamId: Int): Boolean {
        return streamId <= this.localStreamId.get()
    }

    protected fun isRemoteStreamClosed(streamId: Int): Boolean {
        return streamId <= getLastRemoteStreamId()
    }

    override fun onStreamFailure(streamId: Int, error: Int, reason: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionFailure(error: Int, reason: String) {
        onConnectionFailure(error, reason, discard())
    }

    protected fun onConnectionFailure(error: Int, reason: String, result: Consumer<Result<Void>>) {
        notifyFailure(this,
            IOException(String.format("%d/%s", error, reason)),
            Consumer { close(error, reason, result) })
    }

    protected fun notifyFailure(connection: Http2Connection, throwable: Throwable, consumer: Consumer<Result<Void>>) {
        try {
            listener.onFailure(connection, throwable, consumer)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
        }
    }

    fun updateRecvWindow(delta: Int): Int {
        return recvWindow.getAndAdd(delta)
    }

    fun updateSendWindow(delta: Int): Int {
        return sendWindow.getAndAdd(delta)
    }

    fun getSendWindow(): Int {
        return sendWindow.get()
    }

    fun getRecvWindow(): Int {
        return recvWindow.get()
    }

    fun updateStreamCount(local: Boolean, deltaStreams: Int, deltaClosing: Int) {
        if (local) {
            localStreamCount.addAndGet(deltaStreams)
        } else {
            remoteStreamCount.add(deltaStreams, deltaClosing)
        }
    }

    override fun toString(): String {
        return java.lang.String.format(
            "%s@%x{l:%s <-> r:%s,sendWindow=%s,recvWindow=%s,streams=%d,%s,%s}",
            this::class.java.simpleName,
            hashCode(),
            tcpConnection.localAddress,
            tcpConnection.remoteAddress,
            sendWindow,
            recvWindow,
            streams.size,
            closeState.get(),
            closeFrame?.toString()
        )
    }
}