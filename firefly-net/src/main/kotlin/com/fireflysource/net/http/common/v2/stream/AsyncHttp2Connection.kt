package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.common.concurrent.AtomicBiInteger
import com.fireflysource.common.concurrent.Atomics
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import java.io.IOException
import java.nio.channels.ClosedChannelException
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
        val defaultHttp2ConnectionListener = Http2Connection.Listener.Adapter()
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
    private var streamIdleTimeout = tcpConnection.maxIdleTime
    protected var initialSessionRecvWindow: Int = config.initialSessionRecvWindow
    private var pushEnabled: Boolean = false
    private var closeFrame: GoAwayFrame? = null

    private val flusher = FrameEntryFlusher()


    private inner class FrameEntryFlusher {

        private val frameEntryChannel = Channel<FrameEntry>(Channel.UNLIMITED)

        init {
            launchEntryFlushJob()
        }

        private fun launchEntryFlushJob(): Job {
            val entryFlushJob = launchGlobally(tcpConnection.coroutineDispatcher) {
                while (true) {
                    when (val frameEntry = frameEntryChannel.receive()) {
                        is ControlFrameEntry -> {
                            try {
                                val length = flushControlFrameEntry(frameEntry)
                                frameEntry.result.accept(Result(true, length, null))
                            } catch (e: ClosedChannelException) {
                                log.warn(e) { "The TCP connection closed" }
                                frameEntry.result.accept(createFailedResult(-1, e))
                            } catch (e: Exception) {
                                log.error(e) { "flush control frame exception" }
                                frameEntry.result.accept(createFailedResult(-1, e))
                            }
                        }
                        is DataFrameEntry -> {
                            // TODO
                        }
                    }
                }
            }
            tcpConnection.onClose { entryFlushJob.cancel() }
            return entryFlushJob
        }

        private suspend fun flushControlFrameEntry(frameEntry: ControlFrameEntry): Long {
            val stream = frameEntry.stream
            var writeBytes = 0L
            frameLoop@ for (frame in frameEntry.frames) {
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

                val bytes = tcpConnection.write(byteBuffers, 0, byteBuffers.size).await()
                writeBytes += bytes

                when (frame.type) {
                    FrameType.HEADERS -> {
                        val headersFrame = frame as HeadersFrame
                        if (stream != null && stream is AsyncHttp2Stream) {
                            onStreamOpened(stream)
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
                        tcpConnection.close {
                            log.info { "Send go away frame and close TCP connection success" }
                        }
                    }
                    FrameType.WINDOW_UPDATE -> {
                        flowControl.windowUpdate(this@AsyncHttp2Connection, stream, frame as WindowUpdateFrame)
                    }
                    else -> {
                        // ignore the other control frame types
                    }
                }
            }

            return writeBytes
        }

        fun sendControlFrame(stream: Stream?, vararg frames: Frame): CompletableFuture<Long> {
            val future = CompletableFuture<Long>()
            val frameEntry = ControlFrameEntry(stream, arrayOf(*frames), futureToConsumer(future))
            frameEntryChannel.offer(frameEntry)
            return future
        }
    }

    fun sendControlFrame(stream: Stream?, vararg frames: Frame): CompletableFuture<Long> =
        flusher.sendControlFrame(stream, *frames)

    fun launchParserJob(parser: Parser): Job {
        val parsingDataJob = launchGlobally(tcpConnection.coroutineDispatcher) {
            val inputChannel = tcpConnection.inputChannel
            recvLoop@ while (true) {
                val buffer = inputChannel.receive()
                parsingLoop@ while (buffer.hasRemaining()) {
                    parser.parse(buffer)
                }
            }
        }
        tcpConnection.onClose {
            parsingDataJob.cancel()
        }
        return parsingDataJob
    }

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_2

    override fun getTcpConnection(): TcpConnection = tcpConnection


    // stream management
    override fun getStreams(): MutableCollection<Stream> = http2StreamMap.values

    override fun getStream(streamId: Int): Stream? = http2StreamMap[streamId]

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
            onStreamClosed(stream)
            flowControl.onStreamDestroyed(stream)
            log.debug { "Removed $stream" }
        }
    }

    private fun getNextStreamId(): Int = localStreamId.getAndAdd(2)

    protected fun createLocalStream(id: Int, listener: Stream.Listener): Stream {
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

    protected fun createRemoteStream(streamId: Int): Stream? {
        // SPEC: exceeding max concurrent streams treated as stream error.
        if (checkMaxRemoteStreams(streamId)) {
            return null
        }
        val stream: Stream = newStream(streamId, false)
        // SPEC: duplicate stream treated as connection error.
        return if (http2StreamMap.putIfAbsent(streamId, stream) == null) {
            updateLastRemoteStreamId(streamId)
            stream.idleTimeout = streamIdleTimeout
            flowControl.onStreamCreated(stream)
            log.debug { "Created remote $stream" }
            stream
        } else {
            remoteStreamCount.addAndGetHi(-1)
            onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "duplicate_stream")
            null
        }
    }

    private fun checkMaxRemoteStreams(streamId: Int): Boolean {
        while (true) {
            val encoded = remoteStreamCount.get()
            val remoteCount = AtomicBiInteger.getHi(encoded)
            val remoteClosing = AtomicBiInteger.getLo(encoded)
            val maxCount: Int = maxRemoteStreams
            if (maxCount >= 0 && remoteCount - remoteClosing >= maxCount) {
                reset(ResetFrame(streamId, ErrorCode.REFUSED_STREAM_ERROR.code), discard())
                return true
            }
            if (remoteStreamCount.compareAndSet(encoded, remoteCount + 1, remoteClosing)) {
                break
            }
        }
        return false
    }

    protected open fun onStreamOpened(stream: Stream?) {}

    protected open fun onStreamClosed(stream: Stream?) {}

    protected open fun notifyNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener? {
        return try {
            listener.onNewStream(stream, frame)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
            null
        }
    }

    protected fun newStream(streamId: Int, local: Boolean): Stream {
        return AsyncHttp2Stream(this, streamId, local)
    }

    private fun updateLastRemoteStreamId(streamId: Int) {
        Atomics.updateMax(lastRemoteStreamId, streamId)
    }

    fun updateStreamCount(local: Boolean, deltaStreams: Int, deltaClosing: Int) {
        if (local) {
            localStreamCount.addAndGet(deltaStreams)
        } else {
            remoteStreamCount.add(deltaStreams, deltaClosing)
        }
    }

    fun isClientStream(streamId: Int) = (streamId and 1 == 1)

    protected fun getLastRemoteStreamId(): Int {
        return lastRemoteStreamId.get()
    }

    protected fun isLocalStreamClosed(streamId: Int): Boolean {
        return streamId <= this.localStreamId.get()
    }

    protected fun isRemoteStreamClosed(streamId: Int): Boolean {
        return streamId <= getLastRemoteStreamId()
    }

    override fun onStreamFailure(streamId: Int, error: Int, reason: String) {
        val stream = getStream(streamId)
        if (stream != null && stream is AsyncHttp2Stream) {
            stream.process(FailureFrame(error, reason), discard())
        } else {
            reset(ResetFrame(streamId, error), discard())
        }
    }


    // reset frame
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

    protected fun notifyReset(http2Connection: Http2Connection, frame: ResetFrame) {
        try {
            listener.onReset(http2Connection, frame)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
        }
    }

    protected abstract fun onResetForUnknownStream(frame: ResetFrame)

    protected fun reset(frame: ResetFrame, result: Consumer<Result<Void>>) {
        sendControlFrame(getStream(frame.streamId), frame)
            .thenAccept { result.accept(SUCCESS) }
            .exceptionally {
                result.accept(createFailedResult(it))
                null
            }
    }


    // headers frame
    abstract override fun onHeaders(frame: HeadersFrame)

    protected fun notifyHeaders(stream: AsyncHttp2Stream, frame: HeadersFrame) {
        try {
            stream.listener.onHeaders(stream, frame)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
        }
    }

    fun push(frame: PushPromiseFrame, promise: Consumer<Result<Stream?>>, listener: Stream.Listener) {
        val promiseStreamId = localStreamId.getAndAdd(2)
        val pushStream = createLocalStream(promiseStreamId, listener)
        val pushPromiseFrame = PushPromiseFrame(frame.streamId, promiseStreamId, frame.metaData)
        sendControlFrame(pushStream, pushPromiseFrame)
            .thenAccept { promise.accept(Result(true, pushStream, null)) }
            .exceptionally {
                promise.accept(Result(false, null, it))
                null
            }
    }


    // data frame
    override fun onData(frame: DataFrame) {
        // TODO
    }


    // priority frame
    override fun priority(frame: PriorityFrame, result: Consumer<Result<Void>>): Int {
        val stream = http2StreamMap[frame.streamId]
        if (stream == null) {
            val newStreamId = getNextStreamId()
            val newFrame = PriorityFrame(newStreamId, frame.parentStreamId, frame.weight, frame.isExclusive)
            sendControlFrame(null, newFrame)
                .thenAccept { result.accept(SUCCESS) }
                .exceptionally {
                    result.accept(createFailedResult(it))
                    null
                }
            return newStreamId
        } else {
            sendControlFrame(stream, frame)
                .thenAccept { result.accept(SUCCESS) }
                .exceptionally {
                    result.accept(createFailedResult(it))
                    null
                }
            return stream.id
        }
    }

    override fun onPriority(frame: PriorityFrame) {
        log.debug { "Received $frame" }
    }


    // settings frame
    override fun settings(frame: SettingsFrame, result: Consumer<Result<Void>>) {
        sendControlFrame(null, frame)
            .thenAccept { result.accept(SUCCESS) }
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


    // window update
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


    // ping frame
    override fun ping(frame: PingFrame, result: Consumer<Result<Void>>) {
        if (frame.isReply) {
            result.accept(createFailedResult(IllegalArgumentException("The reply must be false")))
        } else {
            sendControlFrame(null, frame)
                .thenAccept { result.accept(SUCCESS) }
                .exceptionally {
                    result.accept(createFailedResult(it))
                    null
                }
        }
    }

    override fun onPing(frame: PingFrame) {
        if (frame.isReply) {
            notifyPing(this, frame)
        } else {
            sendControlFrame(null, PingFrame(frame.payload, true))
        }
    }

    protected fun notifyPing(connection: Http2Connection, frame: PingFrame) {
        try {
            listener.onPing(connection, frame)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
        }
    }


    // close connection
    override fun close(error: Int, reason: String, result: Consumer<Result<Void>>): Boolean {
        while (true) {
            when (val current: CloseState = closeState.get()) {
                CloseState.NOT_CLOSED -> {
                    if (closeState.compareAndSet(current, CloseState.LOCALLY_CLOSED)) {
                        val goAwayFrame = newGoAwayFrame(CloseState.LOCALLY_CLOSED, error, reason)
                        closeFrame = goAwayFrame
                        sendControlFrame(null, goAwayFrame)
                            .thenAccept { result.accept(SUCCESS) }
                            .exceptionally {
                                result.accept(createFailedResult(it))
                                null
                            }
                        return true
                    }
                }
                else -> {
                    log.debug { "Ignoring close $error/$reason, already closed" }
                    result.accept(SUCCESS)
                    return false
                }
            }
        }
    }

    suspend fun close(error: Int, reason: String): Boolean {
        val future = CompletableFuture<Void>()
        val success = close(error, reason, futureToConsumer(future))
        future.await()
        return success
    }

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


    // go away frame
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


    open fun onFrame(frame: Frame) {
        onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "upgrade")
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


sealed class FrameEntry
class ControlFrameEntry(val stream: Stream?, val frames: Array<Frame>, val result: Consumer<Result<Long>>) :
    FrameEntry()

class DataFrameEntry(val stream: Stream, val frames: Array<DataFrame>, val result: Consumer<Result<Long>>) :
    FrameEntry()