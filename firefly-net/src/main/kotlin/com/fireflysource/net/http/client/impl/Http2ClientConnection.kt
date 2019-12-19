package com.fireflysource.net.http.client.impl

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
    private val listener: Http2Connection.Listener = DefaultHttp2ConnectionListener()
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onResetForUnknownStream(frame: ResetFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        TODO("not implemented")
    }
}