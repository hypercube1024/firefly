package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.common.v2.frame.*
import com.fireflysource.net.http.common.v2.frame.CloseState.*
import com.fireflysource.net.http.common.v2.frame.CloseState.Event.*
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class AsyncHttp2Stream(
    private val asyncHttp2Connection: AsyncHttp2Connection,
    private val id: Int,
    private val local: Boolean,
    private val listener: Stream.Listener
) : Stream, Closeable {

    companion object {
        private val log = SystemLogger.create(AsyncHttp2Stream::class.java)
    }

    private val sendWindow = AtomicInteger()
    private val recvWindow = AtomicInteger()
    private val attributes: ConcurrentMap<String, Any> by lazy { ConcurrentHashMap<String, Any>() }
    private val closeState = AtomicReference(NOT_CLOSED)
    private val createTime = System.currentTimeMillis()

    private var dataLength = Long.MIN_VALUE
    private var localReset = false
    private var remoteReset = false

    fun updateSendWindow(delta: Int): Int {
        return sendWindow.getAndAdd(delta)
    }

    fun updateRecvWindow(delta: Int): Int {
        return recvWindow.getAndAdd(delta)
    }

    fun getSendWindow(): Int {
        return sendWindow.get()
    }

    fun getRecvWindow(): Int {
        return recvWindow.get()
    }

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

    private fun onHeaders(frame: HeadersFrame, result: Consumer<Result<Void>>) {
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

    private fun onData(frame: DataFrame, result: Consumer<Result<Void>>) {
        // TODO
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

    override fun getId(): Int = id

    override fun getHttp2Connection(): Http2Connection = asyncHttp2Connection

    override fun headers(frame: HeadersFrame, result: Consumer<Result<Void>>) = sendControlFrame(frame, result)

    override fun push(frame: PushPromiseFrame, promise: Consumer<Result<Stream>>, listener: Stream.Listener) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun data(frame: DataFrame, result: Consumer<Result<Void>>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun reset(frame: ResetFrame, result: Consumer<Result<Void>>) {
        if (isReset) {
            result.accept(Result.createFailedResult(IllegalStateException("The stream: $id is reset")))
            return
        }
        localReset = true
        sendControlFrame(frame, result)
    }

    private fun sendControlFrame(frame: Frame, result: Consumer<Result<Void>>) {
        asyncHttp2Connection.sendControlFrame(this, frame)
            .thenAccept { result.accept(Result.SUCCESS) }
            .exceptionally {
                result.accept(Result.createFailedResult(it))
                null
            }
    }

    override fun getAttribute(key: String): Any? = attributes[key]

    override fun setAttribute(key: String, value: Any) {
        attributes[key] = value
    }

    override fun removeAttribute(key: String): Any? = attributes.remove(key)

    override fun isReset(): Boolean {
        return localReset || remoteReset
    }

    override fun isClosed(): Boolean {
        return closeState.get() == CLOSED
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

    override fun getIdleTimeout(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setIdleTimeout(idleTimeout: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        val oldState = closeState.getAndSet(CLOSED)
        if (oldState != CLOSED) {
            val deltaClosing = if (oldState == CLOSING) -1 else 0
            asyncHttp2Connection.updateStreamCount(local, -1, deltaClosing)
            notifyClosed(this)
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