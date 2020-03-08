package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.common.`object`.Assert
import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.discard
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.common.v2.frame.*
import com.fireflysource.net.http.common.v2.frame.CloseState.*
import com.fireflysource.net.http.common.v2.frame.CloseState.Event.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.io.Closeable
import java.io.IOException
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class AsyncHttp2Stream(
    private val asyncHttp2Connection: AsyncHttp2Connection,
    private val id: Int,
    private val local: Boolean,
    var listener: Stream.Listener = defaultStreamListener
) : Stream, Closeable {

    companion object {
        private val log = SystemLogger.create(AsyncHttp2Stream::class.java)
        val defaultStreamListener = Stream.Listener.Adapter()
    }

    private val attributes: ConcurrentHashMap<String, Any> by lazy { ConcurrentHashMap<String, Any>() }

    private val sendWindow = AtomicInteger()
    private val recvWindow = AtomicInteger()
    private val level = AtomicInteger()

    private val closeState = AtomicReference(NOT_CLOSED)
    private var localReset = false
    private var remoteReset = false

    private val createTime = System.currentTimeMillis()
    private var lastActiveTime = createTime
    private var idleTimeout: Long = 0
    private var idleCheckJob: Job? = null

    private var dataLength = Long.MIN_VALUE

    val stashedDataFrames = LinkedList<DataFrameEntry>()


    override fun getId(): Int = id

    override fun getHttp2Connection(): Http2Connection = asyncHttp2Connection

    override fun getIdleTimeout(): Long = idleTimeout

    override fun setIdleTimeout(idleTimeout: Long) {
        if (idleTimeout > 0) {
            this.idleTimeout = idleTimeout
            val job = idleCheckJob
            idleCheckJob = if (job == null) {
                launchIdleCheckJob()
            } else {
                job.cancel(CancellationException("Set the new idle timeout. id: $id"))
                launchIdleCheckJob()
            }
        }
    }

    private fun noIdle() {
        lastActiveTime = System.currentTimeMillis()
    }

    private fun launchIdleCheckJob() = asyncHttp2Connection.coroutineScope.launch {
        while (true) {
            val duration = Duration.ofSeconds(idleTimeout)
            delay(duration)
            val idle = System.currentTimeMillis() - lastActiveTime
            if (idle >= duration.toMillis()) {
                notifyIdleTimeout()
                break
            }
        }
    }

    private fun notifyIdleTimeout() {
        try {
            val reset = listener.onIdleTimeout(this, TimeoutException("Stream idle timeout"))
            if (reset) {
                val frame = ResetFrame(id, ErrorCode.CANCEL_STREAM_ERROR.code)
                reset(frame, discard())
            }
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
        }
    }

    override fun getAttribute(key: String): Any? = attributes[key]

    override fun setAttribute(key: String, value: Any) {
        attributes[key] = value
    }

    override fun removeAttribute(key: String): Any? = attributes.remove(key)


    fun process(frame: Frame, result: Consumer<Result<Void>>) {
        when (frame.type) {
            FrameType.HEADERS -> onHeaders(frame as HeadersFrame, result)
            FrameType.DATA -> onData(frame as DataFrame, result)
            FrameType.RST_STREAM -> onReset(frame as ResetFrame, result)
            FrameType.PUSH_PROMISE -> {
                // They are closed when receiving an end-stream DATA frame.
                // Pushed streams implicitly locally closed.
                // They are closed when receiving an end-stream DATA frame.
                updateClose(true, AFTER_SEND)
                result.accept(Result.SUCCESS)
            }
            FrameType.WINDOW_UPDATE -> result.accept(Result.SUCCESS)
            FrameType.FAILURE -> notifyFailure(this, frame as FailureFrame, result)
            else -> throw UnsupportedOperationException()
        }
    }

    // header frame
    override fun headers(frame: HeadersFrame, result: Consumer<Result<Void>>) {
        try {
            noIdle()
            Assert.isTrue(frame.streamId == id, "The headers frame id must equal the stream id")
            sendControlFrame(frame, result)
        } catch (e: Exception) {
            result.accept(Result.createFailedResult(e))
        }
    }

    private fun onHeaders(frame: HeadersFrame, result: Consumer<Result<Void>>) {
        noIdle()
        val metaData: MetaData = frame.metaData
        if (metaData.isRequest || metaData.isResponse) {
            val fields = metaData.fields
            var length: Long = -1
            if (fields != null) {
                length = fields.getLongField(HttpHeader.CONTENT_LENGTH.value)
            }
            dataLength = if (length >= 0) length else Long.MIN_VALUE
        }
        if (updateClose(frame.isEndStream, RECEIVED)) {
            asyncHttp2Connection.removeStream(this)
        }
        result.accept(Result.SUCCESS)
    }


    // push promise frame
    override fun push(frame: PushPromiseFrame, promise: Consumer<Result<Stream?>>, listener: Stream.Listener) {
        noIdle()
        asyncHttp2Connection.push(frame, promise, listener)
    }


    // data frame
    override fun data(frame: DataFrame, result: Consumer<Result<Void>>) {
        noIdle()
        asyncHttp2Connection.sendDataFrame(this, frame)
            .thenAccept { result.accept(Result.SUCCESS) }
            .exceptionallyAccept { result.accept(Result.createFailedResult(it)) }
    }

    private fun onData(frame: DataFrame, result: Consumer<Result<Void>>) {
        noIdle()
        if (getRecvWindow() < 0) {
            // It's a bad client, it does not deserve to be treated gently by just resetting the stream.
            asyncHttp2Connection.close(ErrorCode.FLOW_CONTROL_ERROR.code, "stream_window_exceeded", discard())
            result.accept(Result.createFailedResult(IOException("stream_window_exceeded")))
            return
        }

        // SPEC: remotely closed streams must be replied with a reset.
        if (isRemotelyClosed()) {
            reset(ResetFrame(id, ErrorCode.STREAM_CLOSED_ERROR.code), discard())
            result.accept(Result.createFailedResult(IOException("stream_closed")))
            return
        }

        if (isReset) { // Just drop the frame.
            result.accept(Result.createFailedResult(IOException("stream_reset")))
            return
        }

        if (dataLength != Long.MIN_VALUE) {
            dataLength -= frame.remaining()
            if (frame.isEndStream && dataLength != 0L) {
                reset(ResetFrame(id, ErrorCode.PROTOCOL_ERROR.code), discard())
                result.accept(Result.createFailedResult(IOException("invalid_data_length")))
                return
            }
        }

        if (updateClose(frame.isEndStream, RECEIVED)) {
            asyncHttp2Connection.removeStream(this)
        }

        notifyData(this, frame, result)
    }

    fun isRemotelyClosed(): Boolean {
        val state = closeState.get()
        return state === REMOTELY_CLOSED || state === CLOSING
    }

    private fun notifyData(stream: Stream, frame: DataFrame, result: Consumer<Result<Void>>) {
        try {
            listener.onData(stream, frame, result)
        } catch (e: Throwable) {
            log.error(e) { "Failure while notifying listener $listener" }
            result.accept(Result.createFailedResult(e))
        }
    }


    // window update
    fun updateSendWindow(delta: Int): Int = sendWindow.getAndAdd(delta)

    fun getSendWindow(): Int = sendWindow.get()

    fun updateRecvWindow(delta: Int): Int = recvWindow.getAndAdd(delta)

    fun getRecvWindow(): Int = recvWindow.get()

    fun addAndGetLevel(delta: Int): Int = level.addAndGet(delta)

    fun setLevel(level: Int) {
        this.level.set(level)
    }


    // reset frame
    override fun reset(frame: ResetFrame, result: Consumer<Result<Void>>) {
        if (isReset) {
            result.accept(Result.createFailedResult(IllegalStateException("The stream: $id is reset")))
            return
        }
        localReset = true
        sendControlFrame(frame, result)
    }

    override fun isReset(): Boolean {
        return localReset || remoteReset
    }

    private fun onReset(frame: ResetFrame, result: Consumer<Result<Void>>) {
        remoteReset = true
        close()
        asyncHttp2Connection.removeStream(this)
        notifyReset(this, frame, result)
    }

    private fun notifyReset(stream: Stream, frame: ResetFrame, result: Consumer<Result<Void>>) {
        try {
            listener.onReset(stream, frame, result)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
        }
    }

    private fun notifyFailure(stream: Stream, frame: FailureFrame, result: Consumer<Result<Void>>) {
        try {
            listener.onFailure(stream, frame.error, frame.reason, result)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
            result.accept(Result.createFailedResult(e))
        }
    }


    private fun sendControlFrame(frame: Frame, result: Consumer<Result<Void>>) {
        asyncHttp2Connection.sendControlFrame(this, frame)
            .thenAccept { result.accept(Result.SUCCESS) }
            .exceptionallyAccept { result.accept(Result.createFailedResult(it)) }
    }


    // close frame
    override fun close() {
        val oldState = closeState.getAndSet(CLOSED)
        if (oldState != CLOSED) {
            idleCheckJob?.cancel(CancellationException("The stream closed. id: $id"))
            val deltaClosing = if (oldState == CLOSING) -1 else 0
            asyncHttp2Connection.updateStreamCount(local, -1, deltaClosing)
            notifyClosed(this)
        }
    }

    fun updateClose(update: Boolean, event: Event): Boolean {
        log.debug { "Update close for $this update=$update event=$event" }

        if (!update) {
            return false
        }

        return when (event) {
            RECEIVED -> updateCloseAfterReceived()
            BEFORE_SEND -> updateCloseBeforeSend()
            AFTER_SEND -> updateCloseAfterSend()
        }
    }

    override fun isClosed(): Boolean {
        return closeState.get() == CLOSED
    }

    private fun updateCloseAfterReceived(): Boolean {
        while (true) {
            when (val current = closeState.get()) {
                NOT_CLOSED -> if (closeState.compareAndSet(current, REMOTELY_CLOSED)) {
                    return false
                }
                LOCALLY_CLOSING -> {
                    if (closeState.compareAndSet(current, CLOSING)) {
                        asyncHttp2Connection.updateStreamCount(local, 0, 1)
                        return false
                    }
                }
                LOCALLY_CLOSED -> {
                    close()
                    return true
                }
                else -> return false
            }
        }
    }

    private fun updateCloseBeforeSend(): Boolean {
        while (true) {
            when (val current = closeState.get()) {
                NOT_CLOSED -> if (closeState.compareAndSet(current, LOCALLY_CLOSING)) {
                    return false
                }
                REMOTELY_CLOSED -> if (closeState.compareAndSet(current, CLOSING)) {
                    asyncHttp2Connection.updateStreamCount(local, 0, 1)
                    return false
                }
                else -> return false
            }
        }
    }

    private fun updateCloseAfterSend(): Boolean {
        while (true) {
            when (val current = closeState.get()) {
                NOT_CLOSED, LOCALLY_CLOSING -> if (closeState.compareAndSet(current, LOCALLY_CLOSED)) {
                    return false
                }
                REMOTELY_CLOSED, CLOSING -> {
                    close()
                    return true
                }
                else -> return false
            }
        }
    }

    private fun notifyClosed(stream: Stream) {
        try {
            listener.onClosed(stream)
        } catch (x: Throwable) {
            log.info("Failure while notifying listener $listener", x)
        }
    }


    override fun toString(): String {
        return String.format(
            "%s@%x#%d{sendWindow=%s,recvWindow=%s,local=%b,reset=%b/%b,%s,age=%d}",
            "AsyncHttp2Stream",
            hashCode(),
            getId(),
            sendWindow,
            recvWindow,
            local,
            localReset,
            remoteReset,
            closeState,
            (System.currentTimeMillis() - createTime)
        )
    }
}