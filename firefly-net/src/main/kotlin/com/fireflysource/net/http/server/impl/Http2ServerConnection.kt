package com.fireflysource.net.http.server.impl

import com.fireflysource.common.sys.Result.discard
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.v2.decoder.ServerParser
import com.fireflysource.net.http.common.v2.frame.*
import com.fireflysource.net.http.common.v2.stream.*
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.tcp.TcpConnection
import java.util.function.UnaryOperator

class Http2ServerConnection(
    config: HttpConfig,
    tcpConnection: TcpConnection,
    flowControl: FlowControl = BufferedFlowControlStrategy(),
    private val listener: Http2Connection.Listener = Http2ServerConnectionListener()
) : AsyncHttp2Connection(2, config, tcpConnection, flowControl, listener), ServerParser.Listener, HttpServerConnection {

    companion object {
        private val log = SystemLogger.create(Http2ServerConnection::class.java)
    }

    private val parser: ServerParser = ServerParser(this, config.maxDynamicTableSize, config.maxHeaderSize)
    private var connectionListener: HttpServerConnection.Listener = HttpServerConnection.EMPTY_LISTENER

    init {
        parser.init(UnaryOperator.identity())
    }

    fun upgradeHttp2(settingsFrame: SettingsFrame): Stream {
        super.onSettings(settingsFrame)
        val stream = createRemoteStream(1)
        requireNotNull(stream)
        return stream
    }

    override fun begin() {
        if (listener is Http2ServerConnectionListener) {
            listener.connectionListener = connectionListener
        }
        launchParserJob(parser)
    }

    // preface frame
    override fun onPreface() {
        val settings = notifyPreface()
        val settingsFrame = SettingsFrame(settings, false)
        val windowDelta: Int = initialSessionRecvWindow - HttpConfig.DEFAULT_WINDOW_SIZE

        log.info { "HTTP2 server on preface. id: $id, window delta: $windowDelta, settings: $settingsFrame" }
        if (windowDelta > 0) {
            updateRecvWindow(windowDelta)
            sendControlFrame(null, settingsFrame, WindowUpdateFrame(0, windowDelta))
        } else sendControlFrame(null, settingsFrame)
    }

    // headers frame
    override fun onHeaders(frame: HeadersFrame) {
        log.debug { "Received $frame" }

        val streamId = frame.streamId
        if (!isClientStream(streamId)) {
            onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "invalid_stream_id")
            return
        }

        val stream = getStream(streamId)
        val metaData = frame.metaData
        when {
            metaData.isRequest -> onHttpRequest(stream, streamId, frame)
            else -> onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "invalid_request")
        }
    }

    private fun onHttpRequest(stream: Stream?, streamId: Int, frame: HeadersFrame) {
        var remoteStream = stream
        if (remoteStream == null) {
            if (isRemoteStreamClosed(streamId)) {
                onConnectionFailure(ErrorCode.STREAM_CLOSED_ERROR.code, "unexpected_headers_frame")
            } else {
                remoteStream = createRemoteStream(streamId)
                if (remoteStream != null && remoteStream is AsyncHttp2Stream) {
                    onStreamOpened(remoteStream)
                    remoteStream.process(frame, discard())
                    remoteStream.listener = notifyNewStream(remoteStream, frame)
                }
            }
        } else {
            onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "duplicate_stream")
        }
    }

    // promise frame
    override fun onPushPromise(frame: PushPromiseFrame) {
        onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "push_promise")
    }

    override fun onResetForUnknownStream(frame: ResetFrame) {
        val streamId = frame.streamId
        val closed = if (isClientStream(streamId)) isRemoteStreamClosed(streamId) else isLocalStreamClosed(streamId)
        if (closed) {
            notifyReset(this, frame)
        } else {
            onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "unexpected_rst_stream_frame")
        }
    }

    override fun setListener(listener: HttpServerConnection.Listener): HttpServerConnection {
        this.connectionListener = listener
        return this
    }

}