package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.common.concurrent.Atomics
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.v2.frame.WindowUpdateFrame
import java.util.concurrent.atomic.AtomicInteger

class BufferedFlowControlStrategy(
    private val ratio: Float = 0.5f,
    initialStreamRecvWindow: Int = HttpConfig.DEFAULT_WINDOW_SIZE
) : AbstractFlowControlStrategy(initialStreamRecvWindow) {

    companion object {
        private val log = SystemLogger.create(BufferedFlowControlStrategy::class.java)
    }

    private val maxConnectionRecvWindow = AtomicInteger(HttpConfig.DEFAULT_WINDOW_SIZE)
    private val connectionLevel = AtomicInteger()

    override fun onDataConsumed(http2Connection: Http2Connection, stream: Stream?, length: Int) {
        if (length <= 0) return

        val connection = http2Connection as AsyncHttp2Connection
        val level = connectionLevel.addAndGet(length)
        val maxLevel = (maxConnectionRecvWindow.get() * ratio).toInt()
        if (level >= maxLevel) {
            if (connectionLevel.compareAndSet(level, 0)) {
                connection.updateRecvWindow(level)
                log.debug { "Data consumed, $length bytes, updated session recv window by $level/$maxLevel for $http2Connection" }
                connection.sendControlFrame(null, WindowUpdateFrame(0, level))
            } else {
                log.debug { "Data consumed, $length bytes, concurrent session recv window level $level/$maxLevel for $http2Connection" }
            }
        } else {
            log.debug { "Data consumed, $length bytes, session recv window level $level/$maxLevel for $http2Connection" }
        }

        if (stream != null && stream is AsyncHttp2Stream) {
            if (stream.isRemotelyClosed()) {
                log.debug { "Data consumed, $length bytes, ignoring update stream recv window for remotely closed $stream" }
            } else {
                val streamLevel = stream.addAndGetLevel(length)
                val maxStreamLevel = (initialStreamRecvWindow * ratio).toInt()
                if (streamLevel >= maxStreamLevel) {
                    stream.setLevel(0)
                    stream.updateRecvWindow(streamLevel)
                    log.debug { "Data consumed, $length bytes, updated stream recv window by $streamLevel/$maxStreamLevel for $stream" }
                    connection.sendControlFrame(stream, WindowUpdateFrame(stream.getId(), streamLevel))
                } else {
                    log.debug { "Data consumed, $length bytes, stream recv window level $streamLevel/$maxStreamLevel for $stream" }
                }
            }
        }
    }

    override fun windowUpdate(http2Connection: Http2Connection, stream: Stream?, frame: WindowUpdateFrame) {
        super.windowUpdate(http2Connection, stream, frame)
        if (frame.streamId == 0) {
            val connection = http2Connection as AsyncHttp2Connection
            val recvWindow = connection.getRecvWindow()
            Atomics.updateMax(maxConnectionRecvWindow, recvWindow)
        }
    }
}