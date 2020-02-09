package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.v2.frame.WindowUpdateFrame

class SimpleFlowControlStrategy(
    initialStreamRecvWindow: Int = HttpConfig.DEFAULT_WINDOW_SIZE
) : AbstractFlowControlStrategy(initialStreamRecvWindow) {

    companion object {
        private val log = SystemLogger.create(SimpleFlowControlStrategy::class.java)
    }

    override fun onDataConsumed(http2Connection: Http2Connection, stream: Stream?, length: Int) {
        if (length <= 0) return

        // This is the simple algorithm for flow control.
        // This method is called when a whole flow controlled frame has been consumed.
        // We send a WindowUpdate every time, even if the frame was very small.
        val connection = http2Connection as AsyncHttp2Connection
        val sessionFrame = WindowUpdateFrame(0, length)
        connection.updateRecvWindow(length)
        log.debug { "Data consumed, increased session recv window by $length for $connection" }

        var streamFrame: WindowUpdateFrame? = null
        if (stream != null && stream is AsyncHttp2Stream) {
            if (stream.isRemotelyClosed()) {
                log.debug { "Data consumed, ignoring update stream recv window by $length for remotely closed $stream" }
            } else {
                streamFrame = WindowUpdateFrame(stream.id, length)
                stream.updateRecvWindow(length)
                log.debug { "Data consumed, increased stream recv window by $length for $stream" }
            }
        }
        if (streamFrame != null) {
            connection.sendControlFrame(stream, sessionFrame, streamFrame)
        } else {
            connection.sendControlFrame(stream, sessionFrame)
        }
    }

}