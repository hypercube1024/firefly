package com.fireflysource.net.http.server.impl

import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.v2.frame.*
import com.fireflysource.net.http.common.v2.stream.Http2Connection
import com.fireflysource.net.http.common.v2.stream.Stream
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.RoutingContext
import java.util.*
import java.util.function.Consumer

class Http2ServerConnectionListener : Http2Connection.Listener.Adapter() {

    companion object {
        private val log = SystemLogger.create(Http2ServerConnectionListener::class.java)
    }

    var connectionListener: HttpServerConnection.Listener = HttpServerConnection.EMPTY_LISTENER

    private inner class Http2StreamHandler(val stream: Stream) {
        val headers = LinkedList<HeadersFrame>()
        var responseClient100Continue = false
        val trailer = HttpFields()
        var receivedData = false


    }

    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {


        return object : Stream.Listener.Adapter() {

            override fun onHeaders(stream: Stream, frame: HeadersFrame) {

            }

            override fun onData(stream: Stream, frame: DataFrame, result: Consumer<Result<Void>>) {

            }

        }
    }

    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
        log.info { "HTTP2 server connection closed. id: ${http2Connection.id}, frame: $frame" }
    }

    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
        connectionListener.onException(null, failure)
    }

    override fun onReset(http2Connection: Http2Connection, frame: ResetFrame) {
        Optional.ofNullable(http2Connection.getStream(frame.streamId))
            .map { it.getAttribute("routingContext") }
            .map { it as RoutingContext }
            .ifPresent {
                val e = IllegalStateException(ErrorCode.toString(frame.error, "stream exception"))
                connectionListener.onException(it, e)
            }
    }
}