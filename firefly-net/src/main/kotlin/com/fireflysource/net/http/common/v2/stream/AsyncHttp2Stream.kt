package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.v2.frame.*
import com.fireflysource.net.http.common.v2.frame.CloseState.*
import com.fireflysource.net.http.common.v2.frame.CloseState.Event.*
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean
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
    private val localReset = AtomicBoolean()
    private val remoteReset = AtomicBoolean()
    private val closeState = AtomicReference(CloseState.NOT_CLOSED)
    private val createTime = System.currentTimeMillis()

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
        if (localReset.compareAndSet(false, true)) {
            sendControlFrame(frame, result)
        }
    }

    private fun sendControlFrame(frame: Frame, result: Consumer<Result<Void>>) {
        asyncHttp2Connection.sendControlFrame(frame)
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
        return localReset.get() || remoteReset.get()
    }

    override fun isClosed(): Boolean {
        return closeState.get() == CloseState.CLOSED
    }

    fun updateClose(update: Boolean, event: CloseState.Event): Boolean {

        log.debug("Update close for {} update={} event={}", this, update, event)

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
                NOT_CLOSED -> {
                    if (closeState.compareAndSet(current, CloseState.REMOTELY_CLOSED))
                        return false
                }
                LOCALLY_CLOSING -> {
                    if (closeState.compareAndSet(current, CloseState.CLOSING)) {
                        asyncHttp2Connection.updateStreamCount(local, 0, 1)
                        return false
                    }
                }
                LOCALLY_CLOSED -> {
                    close()
                    return true
                }
                else -> {
                    return false
                }
            }
        }
    }

    private fun updateCloseBeforeSend(): Boolean {
        while (true) {
            when (val current = closeState.get()) {
                NOT_CLOSED -> {
                    if (closeState.compareAndSet(current, CloseState.LOCALLY_CLOSING))
                        return false
                }
                REMOTELY_CLOSED -> {
                    if (closeState.compareAndSet(current, CloseState.CLOSING)) {
                        asyncHttp2Connection.updateStreamCount(local, 0, 1)
                        return false
                    }
                }
                else -> {
                    return false
                }
            }
        }
    }

    private fun updateCloseAfterSend(): Boolean {
        while (true) {
            when (val current = closeState.get()) {
                NOT_CLOSED, LOCALLY_CLOSING -> {
                    if (closeState.compareAndSet(current, CloseState.LOCALLY_CLOSED))
                        return false
                }
                REMOTELY_CLOSED, CLOSING -> {
                    close()
                    return true
                }
                else -> {
                    return false
                }
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toString(): String {
        return String.format(
            "%s@%x#%d{sendWindow=%s,recvWindow=%s,reset=%b/%b,%s,age=%d}",
            "AsyncHttp2Stream",
            hashCode(),
            getId(),
            sendWindow,
            recvWindow,
            localReset,
            remoteReset,
            closeState,
            (System.currentTimeMillis() - createTime)
        )
    }
}