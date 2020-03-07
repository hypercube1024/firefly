package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.common.concurrent.Atomics
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.v2.frame.WindowUpdateFrame
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class BufferedFlowControlStrategy(
    private val ratio: Float = 0.5f,
    initialStreamRecvWindow: Int = HttpConfig.DEFAULT_WINDOW_SIZE
) : AbstractFlowControlStrategy(initialStreamRecvWindow) {

    companion object {
        private val log = SystemLogger.create(BufferedFlowControlStrategy::class.java)
    }

    private val maxSessionRecvWindow = AtomicInteger(HttpConfig.DEFAULT_WINDOW_SIZE)
    private val sessionLevel = AtomicInteger()
    private val streamLevels: MutableMap<Int, AtomicInteger> = ConcurrentHashMap<Int, AtomicInteger>()


    override fun onStreamCreated(stream: Stream) {
        super.onStreamCreated(stream)
        streamLevels[stream.id] = AtomicInteger()
    }

    override fun onStreamDestroyed(stream: Stream) {
        streamLevels.remove(stream.id)
        super.onStreamDestroyed(stream)
    }

    override fun onDataConsumed(http2Connection: Http2Connection, stream: Stream?, length: Int) {
        if (length <= 0) return

        val connection = http2Connection as AsyncHttp2Connection
        val connectionLevel = sessionLevel.addAndGet(length)
        val maxConnectionLevel = (maxSessionRecvWindow.get() * ratio).toInt()
        if (connectionLevel > maxConnectionLevel) {
            if (sessionLevel.compareAndSet(connectionLevel, 0)) {
                connection.updateRecvWindow(connectionLevel)
                log.debug(
                    "Data consumed, {} bytes, updated session recv window by {}/{} for {}",
                    length, connectionLevel, maxConnectionLevel, http2Connection.id
                )
                connection.sendControlFrame(null, WindowUpdateFrame(0, connectionLevel))
            } else {
                log.debug(
                    "Data consumed, {} bytes, concurrent session recv window level {}/{} for {}",
                    length, sessionLevel, maxConnectionLevel, http2Connection.id
                )
            }
        } else {
            log.debug(
                "Data consumed, {} bytes, session recv window level {}/{} for {}",
                length, connectionLevel, maxConnectionLevel, http2Connection.id
            )
        }

        if (stream != null && stream is AsyncHttp2Stream) {
            if (stream.isRemotelyClosed()) {
                log.debug { "Data consumed, $length bytes, ignoring update stream recv window for remotely closed $stream" }
            } else {
                val level = streamLevels[stream.id]
                if (level != null) {
                    val streamLevel = level.addAndGet(length)
                    val maxStreamLevel = (initialStreamRecvWindow * ratio).toInt()
                    if (streamLevel > maxStreamLevel) {
                        stream.updateRecvWindow(streamLevel)
                        log.debug { "Data consumed, $length bytes, updated stream recv window by $streamLevel/$maxStreamLevel for $stream" }
                        connection.sendControlFrame(stream, WindowUpdateFrame(stream.getId(), streamLevel))
                        level.set(0)
                    } else {
                        log.debug { "Data consumed, $length bytes, stream recv window level $streamLevel/$maxStreamLevel for $stream" }
                    }
                }
            }
        }
    }

    override fun windowUpdate(http2Connection: Http2Connection, stream: Stream?, frame: WindowUpdateFrame) {
        super.windowUpdate(http2Connection, stream, frame)
        if (frame.streamId <= 0) {
            val connection = http2Connection as AsyncHttp2Connection
            val recvWindow = connection.getRecvWindow()
            Atomics.updateMax(maxSessionRecvWindow, recvWindow)
        }
    }
}