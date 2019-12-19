package com.fireflysource.net.http.server.impl

import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.impl.DefaultHttp2ConnectionListener
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.v2.decoder.ServerParser
import com.fireflysource.net.http.common.v2.frame.*
import com.fireflysource.net.http.common.v2.stream.AsyncHttp2Connection
import com.fireflysource.net.http.common.v2.stream.FlowControl
import com.fireflysource.net.http.common.v2.stream.Http2Connection
import com.fireflysource.net.http.common.v2.stream.SimpleFlowControlStrategy
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.tcp.TcpConnection
import java.util.function.UnaryOperator

class Http2ServerConnection(
    config: HttpConfig,
    tcpConnection: TcpConnection,
    flowControl: FlowControl = SimpleFlowControlStrategy(),
    private val listener: Http2Connection.Listener = DefaultHttp2ConnectionListener()
) : AsyncHttp2Connection(2, config, tcpConnection, flowControl, listener), HttpServerConnection,
    ServerParser.Listener {

    companion object {
        private val log = SystemLogger.create(Http2ServerConnection::class.java)
    }

    private val parser: ServerParser = ServerParser(this, config.maxDynamicTableSize, config.maxHeaderSize)

    init {
        parser.init(UnaryOperator.identity())
        launchParserJob(parser)
    }

    override fun onHeaders(frame: HeadersFrame) {
        // TODO
    }

    override fun onResetForUnknownStream(frame: ResetFrame) {
        // TODO
    }

    override fun onPreface() {
        val settings = notifyPreface(this) ?: mutableMapOf()
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

    private fun notifyPreface(connection: Http2Connection): MutableMap<Int, Int>? {
        return try {
            listener.onPreface(connection)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
            null
        }
    }

    override fun onFrame(frame: Frame) {
        when (frame.type) {
            FrameType.PREFACE -> onPreface()
            FrameType.SETTINGS -> onSettings(
                (frame as SettingsFrame),
                false
            ) // SPEC: the required reply to these SETTINGS frame is the 101 response.
            FrameType.HEADERS -> onHeaders((frame as HeadersFrame))
            else -> super.onFrame(frame)
        }
    }
}