package com.fireflysource.net.http.server.impl

import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.impl.DefaultHttp2ConnectionListener
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.v2.decoder.ServerParser
import com.fireflysource.net.http.common.v2.frame.HeadersFrame
import com.fireflysource.net.http.common.v2.frame.ResetFrame
import com.fireflysource.net.http.common.v2.frame.SettingsFrame
import com.fireflysource.net.http.common.v2.frame.WindowUpdateFrame
import com.fireflysource.net.http.common.v2.stream.AsyncHttp2Connection
import com.fireflysource.net.http.common.v2.stream.FlowControl
import com.fireflysource.net.http.common.v2.stream.Http2Connection
import com.fireflysource.net.http.common.v2.stream.SimpleFlowControlStrategy
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.tcp.TcpConnection
import kotlinx.coroutines.Job
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
    private val receiveDataJob: Job

    init {
        parser.init(UnaryOperator.identity())
        receiveDataJob = launchGlobally(tcpConnection.coroutineDispatcher) {
            val inputChannel = tcpConnection.inputChannel
            recvLoop@ while (true) {
                val buffer = inputChannel.receive()
                parsingLoop@ while (buffer.hasRemaining()) {
                    parser.parse(buffer)
                }
            }
        }
//        tcpConnection.onClose {
//            receiveDataJob.cancel()
//        }
    }

    override fun onHeaders(frame: HeadersFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onResetForUnknownStream(frame: ResetFrame) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
}