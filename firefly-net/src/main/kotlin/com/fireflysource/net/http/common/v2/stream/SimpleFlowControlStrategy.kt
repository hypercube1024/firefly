package com.fireflysource.net.http.common.v2.stream

import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.v2.frame.WindowUpdateFrame

class SimpleFlowControlStrategy(
    initialStreamRecvWindow: Int = HttpConfig.DEFAULT_WINDOW_SIZE
) : AbstractFlowControlStrategy(initialStreamRecvWindow) {

    override fun onStreamDestroyed(stream: Stream) {
        // TODO
    }

    override fun onDataReceived(http2Connection: Http2Connection, stream: Stream, length: Int) {
        // TODO
    }

    override fun onDataConsumed(http2Connection: Http2Connection, stream: Stream, length: Int) {
        // TODO
    }

    override fun windowUpdate(http2Connection: Http2Connection, stream: Stream?, frame: WindowUpdateFrame) {
        // TODO
    }

    override fun onDataSending(stream: Stream, length: Int) {
        // TODO
    }

    override fun onDataSent(stream: Stream, length: Int) {
        // TODO
    }
}