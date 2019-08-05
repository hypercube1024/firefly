package com.fireflysource.net.http.client.impl

import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.Connection
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.v2.decoder.Parser
import com.fireflysource.net.http.common.v2.encoder.Generator
import com.fireflysource.net.http.common.v2.frame.*
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import kotlinx.coroutines.Job
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.UnaryOperator

class Http2ClientConnection(
    private val tcpConnection: TcpConnection,
    private val maxDynamicTableSize: Int = 8192,
    private val maxHeaderSize: Int = 8192,
    private val maxHeaderBlockFragment: Int = 0,
    private val listener: Http2ClientConnectionListener = DefaultHttp2ClientConnectionListener()
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, HttpClientConnection {

    companion object {
        private val log = SystemLogger.create(Http2ClientConnection::class.java)
    }

    private val streamId = AtomicInteger(1)
    private val http2StreamMap: Map<Int, Http2Stream> = ConcurrentHashMap()
    private val generator = Generator(maxDynamicTableSize, maxHeaderBlockFragment)
    private val parser = Parser(object : Parser.Listener {

        override fun onData(frame: DataFrame) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onHeaders(frame: HeadersFrame) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onPriority(frame: PriorityFrame) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onReset(frame: ResetFrame) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onSettings(frame: SettingsFrame) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onPushPromise(frame: PushPromiseFrame) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onPing(frame: PingFrame) {
            launchGlobally(tcpConnection.coroutineDispatcher) {
                if (frame.isReply) {
                    try {
                        listener.onPingFrame(this@Http2ClientConnection, frame)
                    } catch (e: Exception) {
                        log.error(e) { "failure while notifying listener" }
                    }
                } else {
                    val replay = PingFrame(frame.payload, true)
                    sendControlFrame(replay)
                }
            }
        }

        override fun onGoAway(frame: GoAwayFrame) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onWindowUpdate(frame: WindowUpdateFrame) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onStreamFailure(streamId: Int, error: Int, reason: String) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onConnectionFailure(error: Int, reason: String) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }, maxDynamicTableSize, maxHeaderSize)

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

        tcpConnection.onClose {
            receiveDataJob.cancel()
        }
    }


    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_2

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        TODO("not implemented")
    }

    suspend fun newStream(listener: Http2StreamListener): Http2Stream {
        TODO("not implemented")
    }

    suspend fun close(error: Int, payload: String) {
        TODO("not implemented")
    }

    fun sendControlFrame(frame: Frame) {
        val bufList = generator.control(frame).byteBuffers
        tcpConnection.write(bufList, 0, bufList.size, Result.discard())
    }
}