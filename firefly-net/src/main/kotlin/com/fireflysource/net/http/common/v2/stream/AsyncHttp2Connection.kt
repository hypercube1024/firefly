package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.common.concurrent.AtomicBiInteger
import com.fireflysource.common.concurrent.Atomics
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import kotlin.math.min

abstract class AsyncHttp2Connection(
    private val initStreamId: Int,
    private val config: HttpConfig,
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

        private val frameEntryChannel = Channel<FlushFrameMessage>(Channel.UNLIMITED)

        init {
            launchEntryFlushJob()
        }

        private fun launchEntryFlushJob() = tcpConnection.coroutineScope.launch {
            while (true) {
                when (val frameEntry = frameEntryChannel.receive()) {
                    is ControlFrameEntry -> flushControlFrameEntry(frameEntry)
                    is DataFrameEntry -> flushOrStashDataFrameEntry(frameEntry)
                    is OnWindowUpdateMessage -> onWindowUpdateMessage(frameEntry)
                }
            }
        }

        private suspend fun flushOrStashDataFrameEntry(frameEntry: DataFrameEntry) {
            try {
                val http2Stream = frameEntry.stream as AsyncHttp2Stream
                val isEmpty = http2Stream.flushStashedDataFrameEntries()
                if (isEmpty) {
                    val success = flushDataFrame(frameEntry)
                    if (!success) {
                        http2Stream.stashFrameEntry(frameEntry)
                        val dataRemaining = frameEntry.dataRemaining
                        log.debug { "Stash a data frame. remaining: $dataRemaining" }
                    }
                } else {
                    http2Stream.stashFrameEntry(frameEntry)
                }
            } catch (e: Exception) {
                log.error(e) { "flush data frame exception." }
                frameEntry.result.accept(createFailedResult(-1L, e))
            }
        }

        private fun AsyncHttp2Stream.stashFrameEntry(frameEntry: DataFrameEntry) {
            this.stashedDataFrames.offer(frameEntry)
        }

        private suspend fun AsyncHttp2Stream.flushStashedDataFrameEntries(): Boolean {
            val stashedDataFrames = this.stashedDataFrames
            flush@ while (stashedDataFrames.isNotEmpty()) {
                val stashedFrameEntry = stashedDataFrames.peek()
                if (stashedFrameEntry != null) {
                    val success = flushDataFrame(stashedFrameEntry)
                    if (success) {
                        val entry = stashedDataFrames.poll()
                        val dataRemaining = entry.dataRemaining
                        val writtenBytes = entry.writtenBytes
                        log.debug { "Poll a stashed data frame. remaining: ${dataRemaining}, written: $writtenBytes" }
                    } else {
                        break@flush
                    }
                } else {
                    break@flush
                }
            }
            return stashedDataFrames.isEmpty()
        }

        private suspend fun flushDataFrame(frameEntry: DataFrameEntry): Boolean {
            val dataFrame = frameEntry.frame
            val stream = frameEntry.stream as AsyncHttp2Stream
            val dataRemaining = frameEntry.dataRemaining

            val sessionSendWindow = getSendWindow()
            val streamSendWindow = stream.getSendWindow()
            val window = min(streamSendWindow, sessionSendWindow)

            log.debug { "Flush data frame. window: $window, remaining: $dataRemaining" }
            if (window <= 0 && dataRemaining > 0) {
                log.debug { "The sending window not enough. stream: $stream" }
                return false
            }

            val length = min(dataRemaining, window)
            val frameBytes = generator.data(dataFrame, length)
            val dataLength = frameBytes.dataLength
            log.debug { "Before flush data frame. window: $window, remaining: $dataRemaining, dataLength: $dataLength" }

            flowControl.onDataSending(stream, dataLength)
            stream.updateClose(dataFrame.isEndStream, CloseState.Event.BEFORE_SEND)

            val writtenBytes = writeAndFlush(frameBytes.byteBuffers)
            frameEntry.dataRemaining -= dataLength
            frameEntry.writtenBytes += writtenBytes

            flowControl.onDataSent(stream, dataLength)
            val currentRemaining = frameEntry.dataRemaining
            log.debug { "After flush data frame. window: $window, remaining: $currentRemaining, dataLength: $dataLength" }

            return if (currentRemaining == 0) {
                // Only now we can update the close state and eventually remove the stream.
                if (stream.updateClose(dataFrame.isEndStream, CloseState.Event.AFTER_SEND)) {
                    removeStream(stream)
                }
                frameEntry.result.accept(Result(true, frameEntry.writtenBytes, null))
                log.debug { "Flush all data success. stream: $stream" }
                true
            } else {
                log.debug { "Flush data success. stream: $stream, remaining: $currentRemaining" }
                false
            }
        }

        fun onWindowUpdate(stream: Stream?, frame: WindowUpdateFrame) {
            frameEntryChannel.offer(OnWindowUpdateMessage(stream, frame))
        }

        private suspend fun onWindowUpdateMessage(onWindowUpdateMessage: OnWindowUpdateMessage) {
            val (stream, frame) = onWindowUpdateMessage
            flowControl.onWindowUpdate(this@AsyncHttp2Connection, stream, frame)
            if (stream != null) {
                val http2Stream = stream as AsyncHttp2Stream
                log.debug { "Flush stream stashed data frames. stream: $http2Stream" }
                http2Stream.flushStashedDataFrameEntries()
            } else {
                if (frame.isSessionWindowUpdate) {
                    log.debug { "Flush all streams stashed data frames. id: ${tcpConnection.id}" }
                    streams.map { it as AsyncHttp2Stream }.forEach { it.flushStashedDataFrameEntries() }
                }
            }
            log.debug { "Update send window and flush stashed data frame success. frame: $frame" }
        }

        private suspend fun flushControlFrameEntry(frameEntry: ControlFrameEntry) {
            try {
                val length = flushControlFrames(frameEntry)
                frameEntry.result.accept(Result(true, length, null))
            } catch (e: Exception) {
                log.warn(e) { "Flush control frame exception. frames: ${frameEntry.frames.asList()}" }
                frameEntry.result.accept(createFailedResult(-1, e))
            }
        }

        private suspend fun flushControlFrames(frameEntry: ControlFrameEntry): Long {
            val stream = frameEntry.stream
            var writtenBytes = 0L
            frameLoop@ for (frame in frameEntry.frames) {
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

                val byteBuffers = generator.control(frame).byteBuffers
                val bytes = writeAndFlush(byteBuffers)
                writtenBytes += bytes

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

            return writtenBytes
        }

        private suspend fun writeAndFlush(byteBuffers: List<ByteBuffer>): Long {
            return tcpConnection.write(byteBuffers, 0, byteBuffers.size)
                .thenCompose { len -> tcpConnection.flush().thenApply { len } }
                .await()
        }

        fun sendControlFrame(stream: Stream?, vararg frames: Frame): CompletableFuture<Long> {
            val future = CompletableFuture<Long>()
            frameEntryChannel.offer(ControlFrameEntry(stream, arrayOf(*frames), futureToConsumer(future)))
            return future
        }

        fun sendDataFrame(stream: Stream, frame: DataFrame): CompletableFuture<Long> {
            val future = CompletableFuture<Long>()
            frameEntryChannel.offer(DataFrameEntry(stream, frame, futureToConsumer(future)))
            return future
        }
    }

    fun sendControlFrame(stream: Stream?, vararg frames: Frame): CompletableFuture<Long> =
        flusher.sendControlFrame(stream, *frames)

    fun launchParserJob(parser: Parser) = tcpConnection.coroutineScope.launch {
        recvLoop@ while (true) {
            val buffer = try {
                tcpConnection.read().await()
            } catch (e: Exception) {
                break@recvLoop
            }
            parsingLoop@ while (buffer.hasRemaining()) {
                parser.parse(buffer)
            }
        }
    }

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_2

    override fun getTcpConnection(): TcpConnection = tcpConnection


    fun notifyPreface(): MutableMap<Int, Int> {
        return try {
            val settings = listener.onPreface(this) ?: newDefaultSettings()
            val initialWindowSize = settings[SettingsFrame.INITIAL_WINDOW_SIZE] ?: config.initialStreamRecvWindow
            flowControl.updateInitialStreamWindow(this, initialWindowSize, true)
            settings
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
            newDefaultSettings()
        }
    }

    private fun newDefaultSettings(): MutableMap<Int, Int> {
        val settings = HashMap(SettingsFrame.DEFAULT_SETTINGS_FRAME.settings)
        settings[SettingsFrame.INITIAL_WINDOW_SIZE] = config.initialStreamRecvWindow
        settings[SettingsFrame.MAX_CONCURRENT_STREAMS] = config.maxConcurrentStreams
        return settings
    }

    // stream management
    override fun getStreams(): MutableCollection<Stream> = http2StreamMap.values

    override fun getStream(streamId: Int): Stream? = http2StreamMap[streamId]

    override fun newStream(headersFrame: HeadersFrame, promise: Consumer<Result<Stream?>>, listener: Stream.Listener) {
        try {
            val frameStreamId = headersFrame.streamId
            if (frameStreamId == 0) {
                val newHeadersFrame = copyHeadersFrameAndSetCurrentStreamId(headersFrame)
                val stream = createLocalStream(newHeadersFrame.streamId, listener)
                sendNewHeadersFrame(stream, newHeadersFrame, promise)
            } else {
                val stream = createLocalStream(frameStreamId, listener)
                sendNewHeadersFrame(stream, headersFrame, promise)
            }
        } catch (e: Exception) {
            promise.accept(Result(false, null, e))
        }
    }

    private fun copyHeadersFrameAndSetCurrentStreamId(headersFrame: HeadersFrame): HeadersFrame {
        val nextStreamId = getNextStreamId()
        val priority = headersFrame.priority
        val priorityFrame = if (priority != null) {
            PriorityFrame(nextStreamId, priority.parentStreamId, priority.weight, priority.isExclusive)
        } else null
        return HeadersFrame(nextStreamId, headersFrame.metaData, priorityFrame, headersFrame.isEndStream)
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

    private fun getNextStreamId(): Int = getAndIncreaseStreamId(localStreamId, initStreamId)

    private fun getCurrentLocalStreamId(): Int = localStreamId.get()

    private fun createLocalStream(streamId: Int, listener: Stream.Listener): Stream {
        checkMaxLocalStreams()
        val stream = AsyncHttp2Stream(this, streamId, true, listener)
        if (http2StreamMap.putIfAbsent(streamId, stream) == null) {
            stream.idleTimeout = streamIdleTimeout
            flowControl.onStreamCreated(stream) // TODO before preface
            log.debug { "Created local $stream" }
            return stream
        } else {
            localStreamCount.decrementAndGet()
            throw IllegalStateException("Duplicate stream $streamId")
        }
    }

    private fun checkMaxLocalStreams() {
        val maxCount = maxLocalStreams
        if (maxCount > 0) {
            while (true) {
                val localCount = localStreamCount.get()
                if (localCount >= maxCount) {
                    throw IllegalStateException("Max local stream count $localCount exceeded $maxCount")
                }
                if (localStreamCount.compareAndSet(localCount, localCount + 1)) {
                    break
                }
            }
        } else {
            localStreamCount.incrementAndGet()
        }
    }

    protected fun createRemoteStream(streamId: Int): Stream? {
        // SPEC: exceeding max concurrent streams treated as stream error.
        if (!checkMaxRemoteStreams(streamId)) {
            return null
        }
        val stream: Stream = AsyncHttp2Stream(this, streamId, false)
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
                return false
            }
            if (remoteStreamCount.compareAndSet(encoded, remoteCount + 1, remoteClosing)) {
                break
            }
        }
        return true
    }

    protected open fun onStreamOpened(stream: Stream) {}

    protected open fun onStreamClosed(stream: Stream) {}

    protected open fun notifyNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
        return try {
            listener.onNewStream(stream, frame)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
            AsyncHttp2Stream.defaultStreamListener
        }
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

    private fun getLastRemoteStreamId(): Int {
        return lastRemoteStreamId.get()
    }

    protected fun isLocalStreamClosed(streamId: Int): Boolean {
        return streamId <= getCurrentLocalStreamId()
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
        if (stream != null && stream is AsyncHttp2Stream) {
            stream.process(frame, discard())
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
        val promiseStreamId = getNextStreamId()
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
        onData(frame, discard())
    }

    private fun onData(frame: DataFrame, result: Consumer<Result<Void>>) {
        log.debug { "Received $frame" }
        val streamId = frame.streamId
        val stream = getStream(streamId)

        // SPEC: the session window must be updated even if the stream is null.
        // The flow control length includes the padding bytes.
        val flowControlLength = frame.remaining() + frame.padding()
        flowControl.onDataReceived(this, stream, flowControlLength)

        if (stream != null) {
            if (getRecvWindow() < 0) {
                onConnectionFailure(ErrorCode.FLOW_CONTROL_ERROR.code, "session_window_exceeded", result)
            } else {
                val dataResult = Consumer<Result<Void>> { r ->
                    flowControl.onDataConsumed(this@AsyncHttp2Connection, stream, flowControlLength)
                    result.accept(r)
                }
                val http2Stream = stream as AsyncHttp2Stream
                http2Stream.process(frame, dataResult)
            }
        } else {
            log.debug("Stream #{} not found", streamId)
            // We must enlarge the session flow control window,
            // otherwise, the other requests will be stalled.
            flowControl.onDataConsumed(this, null, flowControlLength)
            val local = (streamId and 1) == (getCurrentLocalStreamId() and 1)
            val closed = if (local) isLocalStreamClosed(streamId) else isRemoteStreamClosed(streamId)
            if (closed) reset(ResetFrame(streamId, ErrorCode.STREAM_CLOSED_ERROR.code), result)
            else onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "unexpected_data_frame", result)
        }
    }

    fun sendDataFrame(stream: Stream, frame: DataFrame) = flusher.sendDataFrame(stream, frame)


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

    private fun notifySettings(connection: Http2Connection, frame: SettingsFrame) {
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
        if (frame.isStreamWindowUpdate) {
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
            val sessionSendWindow = getSendWindow()
            if (MathUtils.sumOverflows(sessionSendWindow, windowDelta)) {
                onConnectionFailure(ErrorCode.FLOW_CONTROL_ERROR.code, "invalid_flow_control_window")
            } else {
                onWindowUpdate(null, frame)
            }
        }
    }

    fun onWindowUpdate(stream: Stream?, frame: WindowUpdateFrame) {
        flusher.onWindowUpdate(stream, frame)
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
        log.debug { "Received $frame" }
        if (frame.isReply) {
            notifyPing(this, frame)
        } else {
            sendControlFrame(null, PingFrame(frame.payload, true))
        }
    }

    private fun notifyPing(connection: Http2Connection, frame: PingFrame) {
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

    override fun closeFuture(): CompletableFuture<Void> {
        val future = CompletableFuture<Void>()
        close(ErrorCode.NO_ERROR.code, "no_error", futureToConsumer(future))
        return future
    }

    override fun close() {
        closeFuture()
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

    private fun notifyClose(connection: Http2Connection, frame: GoAwayFrame, consumer: Consumer<Result<Void>>) {
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

    private fun notifyFailure(connection: Http2Connection, throwable: Throwable, consumer: Consumer<Result<Void>>) {
        try {
            listener.onFailure(connection, throwable, consumer)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
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

fun getAndIncreaseStreamId(id: AtomicInteger, initStreamId: Int) = id.getAndUpdate { prev ->
    val currentId = prev + 2
    if (currentId <= 0) initStreamId else currentId
}

sealed class FlushFrameMessage
class ControlFrameEntry(val stream: Stream?, val frames: Array<Frame>, val result: Consumer<Result<Long>>) :
    FlushFrameMessage()

class DataFrameEntry(val stream: Stream, val frame: DataFrame, val result: Consumer<Result<Long>>) :
    FlushFrameMessage() {
    var dataRemaining = frame.remaining()
    var writtenBytes: Long = 0
}

data class OnWindowUpdateMessage(val stream: Stream?, val frame: WindowUpdateFrame) : FlushFrameMessage()