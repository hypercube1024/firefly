package com.fireflysource.net.http.client.impl

import com.fireflysource.common.sys.Result.discard
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.HttpConfig.DEFAULT_WINDOW_SIZE
import com.fireflysource.net.http.common.v2.decoder.Parser
import com.fireflysource.net.http.common.v2.frame.*
import com.fireflysource.net.http.common.v2.stream.*
import com.fireflysource.net.tcp.TcpConnection
import java.util.concurrent.CompletableFuture
import java.util.function.UnaryOperator

class Http2ClientConnection(
    config: HttpConfig,
    tcpConnection: TcpConnection,
    flowControl: FlowControl = SimpleFlowControlStrategy(),
    private val listener: Http2Connection.Listener = defaultHttp2ConnectionListener
) : AsyncHttp2Connection(1, config, tcpConnection, flowControl, listener), HttpClientConnection {

    companion object {
        private val log = SystemLogger.create(Http2ClientConnection::class.java)
    }

    private val parser: Parser = Parser(this, config.maxDynamicTableSize, config.maxHeaderSize)

    init {
        parser.init(UnaryOperator.identity())
        sendConnectionPreface(config)
        launchParserJob(parser)
    }

    private fun sendConnectionPreface(config: HttpConfig) {
        val settings = listener.onPreface(this) ?: mutableMapOf()
        settings.computeIfAbsent(SettingsFrame.INITIAL_WINDOW_SIZE) { config.initialStreamRecvWindow }
        settings.computeIfAbsent(SettingsFrame.MAX_CONCURRENT_STREAMS) { config.maxConcurrentPushedStreams }

        val maxFrameLength = settings[SettingsFrame.MAX_FRAME_SIZE]
        if (maxFrameLength != null) {
            parser.maxFrameLength = maxFrameLength
        }

        val prefaceFrame = PrefaceFrame()
        val settingsFrame = SettingsFrame(settings, false)
        val windowDelta = initialSessionRecvWindow - DEFAULT_WINDOW_SIZE
        if (windowDelta > 0) {
            val windowUpdateFrame = WindowUpdateFrame(0, windowDelta)
            updateRecvWindow(windowDelta)
            sendControlFrame(null, prefaceFrame, settingsFrame, windowUpdateFrame)
                .thenAccept { log.info { "send connection preface success. $it" } }
                .exceptionally {
                    log.error(it) { "send connection preface exception" }
                    null
                }
        } else {
            sendControlFrame(null, prefaceFrame, settingsFrame)
                .thenAccept { log.info { "send connection preface success. $it" } }
                .exceptionally {
                    log.error(it) { "send connection preface exception" }
                    null
                }
        }
    }

    override fun onHeaders(frame: HeadersFrame) {
        log.debug { "Received $frame" }

        // HEADERS can be received for normal and pushed responses.
        val streamId = frame.streamId
        val stream = getStream(streamId)
        if (stream != null && stream is AsyncHttp2Stream) {
            val metaData = frame.metaData
            if (metaData.isRequest) {
                onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "invalid_response")
            } else {
                stream.process(frame, discard())
                notifyHeaders(stream, frame)
            }
        } else {
            log.debug { "Stream: $streamId not found" }
            if (isClientStream(streamId)) {
                // The normal stream. Headers or trailers arriving after the stream has been reset are ignored.
                if (!isLocalStreamClosed(streamId)) {
                    onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "unexpected_headers_frame")
                }
            } else {
                // The pushed stream. Headers or trailers arriving after the stream has been reset are ignored.
                if (!isRemoteStreamClosed(streamId)) {
                    onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "unexpected_headers_frame")
                }
            }
        }
    }

    override fun onResetForUnknownStream(frame: ResetFrame) {
        val streamId = frame.streamId
        val closed = if (isClientStream(streamId)) isLocalStreamClosed(streamId) else isRemoteStreamClosed(streamId)
        if (closed) {
            notifyReset(this, frame)
        } else {
            onConnectionFailure(ErrorCode.PROTOCOL_ERROR.code, "unexpected_rst_stream_frame")
        }
    }

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        TODO("not implemented")
    }
}