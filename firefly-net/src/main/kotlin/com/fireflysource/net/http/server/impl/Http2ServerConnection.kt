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
    flowControl: FlowControl = SimpleFlowControlStrategy(),
    listener: Http2Connection.Listener = defaultHttp2ConnectionListener
) : AsyncHttp2Connection(2, config, tcpConnection, flowControl, listener), ServerParser.Listener, HttpServerConnection {

    companion object {
        private val log = SystemLogger.create(Http2ServerConnection::class.java)
    }

    private val parser: ServerParser = ServerParser(this, config.maxDynamicTableSize, config.maxHeaderSize)
    private var connectionListener: HttpServerConnection.Listener? = null

    init {
        parser.init(UnaryOperator.identity())
    }

    override fun begin() {
        launchParserJob(parser)
    }

    // preface frame
    override fun onPreface() {
        val settings = notifyPreface()
        val settingsFrame = SettingsFrame(settings, false)

        var windowFrame: WindowUpdateFrame? = null
        val sessionWindow: Int = initialSessionRecvWindow - HttpConfig.DEFAULT_WINDOW_SIZE
        if (sessionWindow > 0) {
            updateRecvWindow(sessionWindow)
            windowFrame = WindowUpdateFrame(0, sessionWindow)
        }
        if (windowFrame == null) {
            sendControlFrame(null, settingsFrame)
        } else {
            sendControlFrame(null, settingsFrame, windowFrame)
        }
    }

    // SPEC: the required reply to these SETTINGS frame is the 101 response.
    fun onStandardUpgradeFrame(frame: Frame) {
        when (frame.type) {
            FrameType.PREFACE -> onPreface()
            FrameType.SETTINGS -> onSettings((frame as SettingsFrame), false)
            FrameType.HEADERS -> onHeaders((frame as HeadersFrame))
            else -> onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "upgrade")
        }
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
            metaData.isResponse -> onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "invalid_request")
            else -> onTrailers(stream, frame, streamId)
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

    private fun onTrailers(stream: Stream?, frame: HeadersFrame, streamId: Int) {
        if (stream != null && stream is AsyncHttp2Stream) {
            stream.process(frame, discard())
            notifyHeaders(stream, frame)
        } else {
            log.debug { "Stream: $streamId not found" }
            onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "unexpected_headers_frame")
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

    override fun listen(listener: HttpServerConnection.Listener): HttpServerConnection {
        this.connectionListener = listener
        return this
    }
}