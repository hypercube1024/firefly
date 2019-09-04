package com.fireflysource.net.http.client.impl

import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.HttpConfig.DEFAULT_WINDOW_SIZE
import com.fireflysource.net.http.common.v2.frame.PrefaceFrame
import com.fireflysource.net.http.common.v2.frame.SettingsFrame
import com.fireflysource.net.http.common.v2.frame.WindowUpdateFrame
import com.fireflysource.net.http.common.v2.stream.AsyncHttp2Connection
import com.fireflysource.net.http.common.v2.stream.FlowControl
import com.fireflysource.net.http.common.v2.stream.Http2Connection
import com.fireflysource.net.http.common.v2.stream.SimpleFlowControlStrategy
import com.fireflysource.net.tcp.TcpConnection
import java.util.concurrent.CompletableFuture

class Http2ClientConnection(
    config: HttpConfig,
    tcpConnection: TcpConnection,
    private val flowControl: FlowControl = SimpleFlowControlStrategy(),
    private val listener: Http2Connection.Listener = DefaultHttp2ConnectionListener(),
    private val asyncHttp2Connection: AsyncHttp2Connection =
        AsyncHttp2Connection(1, config, tcpConnection, flowControl, listener)
) : Http2Connection by asyncHttp2Connection, HttpClientConnection {

    companion object {
        private val log = SystemLogger.create(Http2ClientConnection::class.java)
    }

    init {
        sendConnectionPreface(config)
    }

    private fun sendConnectionPreface(config: HttpConfig) {
        val settings = listener.onPreface(this)
        settings.computeIfAbsent(SettingsFrame.INITIAL_WINDOW_SIZE) { config.initialStreamRecvWindow }
        settings.computeIfAbsent(SettingsFrame.MAX_CONCURRENT_STREAMS) { config.maxConcurrentPushedStreams }

        val maxFrameLength = settings[SettingsFrame.MAX_FRAME_SIZE]
        if (maxFrameLength != null) {
            asyncHttp2Connection.parser.maxFrameLength = maxFrameLength
        }

        val prefaceFrame = PrefaceFrame()
        val settingsFrame = SettingsFrame(settings, false)
        val windowDelta = asyncHttp2Connection.initialSessionRecvWindow - DEFAULT_WINDOW_SIZE
        if (windowDelta > 0) {
            val windowUpdateFrame = WindowUpdateFrame(0, windowDelta)
            asyncHttp2Connection.updateRecvWindow(windowDelta)
            asyncHttp2Connection.sendControlFrame(null, prefaceFrame, settingsFrame, windowUpdateFrame)
        } else {
            asyncHttp2Connection.sendControlFrame(null, prefaceFrame, settingsFrame)
        }
    }

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        TODO("not implemented")
    }
}