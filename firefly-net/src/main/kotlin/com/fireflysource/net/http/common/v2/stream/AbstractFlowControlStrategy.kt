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
        if (frame.streamId > 0) {
            // The stream may have been removed concurrently.
            if (stream != null && stream is AsyncHttp2Stream) {
                stream.updateSendWindow(frame.windowDelta)
            }
        } else {
            (http2Connection as AsyncHttp2Connection).updateSendWindow(frame.windowDelta)
        }
    }
}