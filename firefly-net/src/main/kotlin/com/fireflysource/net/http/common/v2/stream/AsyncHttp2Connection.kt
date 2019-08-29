package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.common.concurrent.AtomicBiInteger
import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.sys.Result
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.UnaryOperator

class AsyncHttp2Connection(
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

    private val streamId = AtomicInteger(initStreamId)
    private val http2StreamMap = ConcurrentHashMap<Int, Stream>()
    private val localStreamCount = AtomicInteger()
    private val remoteStreamCount = AtomicBiInteger()
    private val lastRemoteStreamId = AtomicInteger()

    private val sendWindow = AtomicInteger(HttpConfig.DEFAULT_WINDOW_SIZE)
    private val recvWindow = AtomicInteger(HttpConfig.DEFAULT_WINDOW_SIZE)

    val parser: Parser = Parser(this, config.maxDynamicTableSize, config.maxHeaderSize)
    private val generator = Generator(config.maxDynamicTableSize, config.maxHeaderBlockFragment)
    private val receiveDataJob: Job

    var maxLocalStreams: Int = -1
    var maxRemoteStreams: Int = -1
    var initialSessionRecvWindow: Int = config.initialSessionRecvWindow
    var pushEnabled: Boolean = false

    init {
        parser.init(UnaryOperator.identity())
        receiveDataJob = launchGlobally(tcpConnection.coroutineDispatcher) {
            val inputChannel = tcpConnection.inputChannel
            recvLoop@ while (true) {
                val buffer = inputChannel.receive()
                parsingLoop@ while (buffer.hasRemaining()) {
                    parser.parse(buffer)
                }
            }
        }
        tcpConnection.onClose {
            receiveDataJob.cancel()
        }
    }

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_2

    override fun getTcpConnection(): TcpConnection = tcpConnection

    override fun newStream(headersFrame: HeadersFrame, promise: Consumer<Result<Stream>>, listener: Stream.Listener) {
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
            sendControlFrame(newHeadersFrame)
            promise.accept(Result(true, stream, null))
        } else {
            val stream = createLocalStream(frameStreamId, listener)
            sendControlFrame(headersFrame)
            promise.accept(Result(true, stream, null))
        }
    }

    private fun getNextStreamId(): Int = streamId.getAndAdd(2)

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

    fun sendControlFrame(vararg frame: Frame): CompletableFuture<Long> {
        val bufList = frame.map { generator.control(it).byteBuffers }.flatten()
        return tcpConnection.write(bufList, 0, bufList.size)
    }

    override fun priority(frame: PriorityFrame, result: Consumer<Result<Void>>): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun settings(frame: SettingsFrame, result: Consumer<Result<Void>>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun ping(frame: PingFrame, result: Consumer<Result<Void>>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStreams(): MutableCollection<Stream> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStream(streamId: Int): Stream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close(error: Int, payload: String, result: Consumer<Result<Void>>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun onData(frame: DataFrame?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onHeaders(frame: HeadersFrame?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPriority(frame: PriorityFrame?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onReset(frame: ResetFrame?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSettings(frame: SettingsFrame?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPushPromise(frame: PushPromiseFrame?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPing(frame: PingFrame) {
        if (frame.isReply) {
            try {
                listener.onPing(this, frame)
            } catch (e: Exception) {
                log.error(e) { "failure while notifying listener" }
            }
        } else {
            val replay = PingFrame(frame.payload, true)
            sendControlFrame(replay)
        }
    }

    override fun onGoAway(frame: GoAwayFrame?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onWindowUpdate(frame: WindowUpdateFrame?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStreamFailure(streamId: Int, error: Int, reason: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionFailure(error: Int, reason: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
}