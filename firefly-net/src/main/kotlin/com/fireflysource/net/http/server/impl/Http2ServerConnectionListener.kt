package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.v2.frame.ErrorCode
import com.fireflysource.net.http.common.v2.frame.GoAwayFrame
import com.fireflysource.net.http.common.v2.frame.HeadersFrame
import com.fireflysource.net.http.common.v2.frame.ResetFrame
import com.fireflysource.net.http.common.v2.stream.Http2Connection
import com.fireflysource.net.http.common.v2.stream.Stream
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.RoutingContext
import java.util.*

class Http2ServerConnectionListener : Http2Connection.Listener.Adapter() {
    var connectionListener: HttpServerConnection.Listener = HttpServerConnection.EMPTY_LISTENER

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

    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}