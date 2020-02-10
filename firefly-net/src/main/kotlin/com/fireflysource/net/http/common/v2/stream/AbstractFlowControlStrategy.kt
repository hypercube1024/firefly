package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.HttpConfig.DEFAULT_WINDOW_SIZE
import com.fireflysource.net.http.common.v2.frame.WindowUpdateFrame

abstract class AbstractFlowControlStrategy(
    private var initialStreamRecvWindow: Int = DEFAULT_WINDOW_SIZE
) : FlowControl {

    companion object {
        private val log = SystemLogger.create(AbstractFlowControlStrategy::class.java)
    }

    private var initialStreamSendWindow: Int = DEFAULT_WINDOW_SIZE

    override fun onStreamCreated(stream: Stream) {
        val http2Stream = stream as AsyncHttp2Stream
        http2Stream.updateSendWindow(initialStreamSendWindow)
        http2Stream.updateRecvWindow(initialStreamRecvWindow)
    }

    override fun onStreamDestroyed(stream: Stream) {
    }

    override fun updateInitialStreamWindow(
        http2Connection: Http2Connection,
        initialStreamWindow: Int,
        local: Boolean
    ) {
        val previousInitialStreamWindow: Int
        if (local) {
            previousInitialStreamWindow = initialStreamRecvWindow
            initialStreamRecvWindow = initialStreamWindow
        } else {
            previousInitialStreamWindow = initialStreamSendWindow
            initialStreamSendWindow = initialStreamWindow
        }

        val delta = initialStreamWindow - previousInitialStreamWindow
        if (delta == 0) return

        http2Connection.streams.forEach { stream ->
            if (local) {
                val http2Stream = stream as AsyncHttp2Stream
                http2Stream.updateRecvWindow(delta)
                log.debug { "Updated initial stream recv window $previousInitialStreamWindow -> $initialStreamWindow for $http2Stream" }
            } else {
                (http2Connection as AsyncHttp2Connection).onWindowUpdate(stream, WindowUpdateFrame(stream.id, delta))
            }
        }
    }

    override fun onWindowUpdate(http2Connection: Http2Connection, stream: Stream?, frame: WindowUpdateFrame) {
        if (frame.isStreamWindowUpdate) {
            // The stream may have been removed concurrently.
            if (stream != null && stream is AsyncHttp2Stream) {
                stream.updateSendWindow(frame.windowDelta)
            }
        } else {
            (http2Connection as AsyncHttp2Connection).updateSendWindow(frame.windowDelta)
        }
    }

    override fun onDataReceived(http2Connection: Http2Connection, stream: Stream?, length: Int) {
        val connection = http2Connection as AsyncHttp2Connection
        var oldSize: Int = connection.updateRecvWindow(-length)
        log.debug { "Data received, $length bytes, updated session recv window $oldSize -> ${oldSize - length} for $connection" }

        if (stream != null && stream is AsyncHttp2Stream) {
            oldSize = stream.updateRecvWindow(-length)
            log.debug { "Data received, $length bytes, updated stream recv window $oldSize -> ${oldSize - length} for $stream" }
        }
    }

    override fun onDataSending(stream: Stream, length: Int) {
        if (length == 0) return

        val http2Stream = stream as AsyncHttp2Stream
        val connection = http2Stream.http2Connection as AsyncHttp2Connection
        val oldSessionWindow = connection.updateSendWindow(-length)
        val newSessionWindow = oldSessionWindow - length
        log.debug { "Sending, session send window $oldSessionWindow -> $newSessionWindow for $connection" }

        val oldStreamWindow = http2Stream.updateSendWindow(-length)
        val newStreamWindow = oldStreamWindow - length
        log.debug { "Sending, stream send window $oldStreamWindow -> $newStreamWindow for $stream" }
    }

    override fun onDataSent(stream: Stream, length: Int) {
    }

    override fun windowUpdate(http2Connection: Http2Connection, stream: Stream?, frame: WindowUpdateFrame) {
    }
}