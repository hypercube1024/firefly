package com.fireflysource.net.http.client.impl

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.Result.discard
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.*
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.HttpConfig.DEFAULT_WINDOW_SIZE
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.common.v2.decoder.Parser
import com.fireflysource.net.http.common.v2.frame.*
import com.fireflysource.net.http.common.v2.stream.*
import com.fireflysource.net.tcp.TcpConnection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.function.UnaryOperator

class Http2ClientConnection(
    private val config: HttpConfig,
    tcpConnection: TcpConnection,
    flowControl: FlowControl = SimpleFlowControlStrategy(),
    listener: Http2Connection.Listener = defaultHttp2ConnectionListener
) : AsyncHttp2Connection(1, config, tcpConnection, flowControl, listener), HttpClientConnection {

    companion object {
        private val log = SystemLogger.create(Http2ClientConnection::class.java)
    }

    private val streamsOpened = AtomicLong()
    private val streamsClosed = AtomicLong()
    private val parser: Parser = Parser(this, config.maxDynamicTableSize, config.maxHeaderSize)

    private val requestChannel = Channel<RequestMessage>(Channel.UNLIMITED)

    init {
        parser.init(UnaryOperator.identity())
        sendConnectionPreface()
        launchParserJob(parser)
        launchHttpExchangeJob()
    }

    private fun launchHttpExchangeJob() = tcpConnection.coroutineScope.launch {
        while (true) {
            val requestMessage = requestChannel.receive()
            try {
                doHttpExchange(requestMessage)
            } catch (e: Exception) {
                requestMessage.response.completeExceptionally(e)
            }
        }
    }

    private suspend fun doHttpExchange(requestMessage: RequestMessage) {
        val metaDataRequest = requestMessage.request
        val trailer = metaDataRequest.trailerSupplier?.get()
        val contentProvider = requestMessage.contentProvider
        val contentHandler = requestMessage.contentHandler
        val future = requestMessage.response
        val lastHeaders = contentProvider == null && trailer == null
        val headersFrame = HeadersFrame(metaDataRequest, null, lastHeaders)

        val response = AsyncHttpClientResponse(MetaData.Response(HttpVersion.HTTP_2, 0, HttpFields()), contentHandler)
        val metaDataResponse = response.response

        val newStream = newStream(headersFrame, object : Stream.Listener.Adapter() {
            override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                val fields = frame.metaData.fields
                metaDataResponse.fields.addAll(fields)
                Optional.ofNullable(fields[HttpHeader.C_STATUS]).map { it.toInt() }.ifPresent { status ->
                    metaDataResponse.status = status
                    metaDataResponse.reason = HttpStatus.getMessage(status)
                }
                if (frame.isEndStream) {
                    future.complete(response)
                }
            }

            override fun onData(stream: Stream, frame: DataFrame, result: Consumer<Result<Void>>) {

                if (frame.isEndStream) {
                    future.complete(response)
                }
            }

            override fun onReset(stream: Stream, frame: ResetFrame) {
                val error = ErrorCode.toString(frame.error, "http2_request_error")
                val exception = IllegalStateException(error)
                future.completeExceptionally(exception)
            }

            override fun onIdleTimeout(stream: Stream, x: Throwable): Boolean {
                val exception = IllegalStateException("http2_stream_timeout")
                future.completeExceptionally(exception)
                return true
            }
        }).await()

        generateContent(contentProvider, trailer, newStream)
        generateTrailer(trailer, newStream)
    }

    private suspend fun generateContent(
        contentProvider: HttpClientContentProvider?,
        trailer: HttpFields?,
        newStream: Stream
    ) {
        if (contentProvider != null) {
            val contentBuffer = BufferUtils.allocate(config.contentBufferSize)
            readLoop@ while (true) {
                val pos = contentBuffer.flipToFill()
                val length = contentProvider.read(contentBuffer).await()
                contentBuffer.flipToFlush(pos)

                when {
                    length > 0 -> {
                        val lastData = length > 0 && trailer == null
                        val dataFrame = DataFrame(newStream.id, contentBuffer, lastData)
                        newStream.data(dataFrame).await()
                        BufferUtils.clear(contentBuffer)
                    }
                    length < 0 -> break@readLoop
                }
            }
        }
    }

    private fun generateTrailer(trailer: HttpFields?, newStream: Stream) {
        if (trailer != null) {
            val trailerMetaData = MetaData.Request(trailer)
            trailerMetaData.isOnlyTrailer = true
            val headersFrameTrailer = HeadersFrame(trailerMetaData, null, true)
            newStream.headers(headersFrameTrailer, discard())
        }
    }

    private fun sendConnectionPreface() {
        val settings = notifyPreface()

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


    // promise frame
    override fun onPushPromise(frame: PushPromiseFrame) {
        log.debug { "Received $frame" }

        val stream = getStream(frame.streamId)
        if (stream == null) {
            log.debug { "Ignoring $frame, stream: ${frame.streamId} not found" }
        } else {
            val pushStream = createRemoteStream(frame.promisedStreamId)
            if (pushStream != null && pushStream is AsyncHttp2Stream) {
                pushStream.process(frame, discard())
                pushStream.listener = notifyPush(stream, pushStream, frame)
            }
        }
    }

    private fun notifyPush(stream: Stream, pushStream: Stream, frame: PushPromiseFrame): Stream.Listener {
        return try {
            val listener = (stream as AsyncHttp2Stream).listener
            listener.onPush(pushStream, frame)
        } catch (e: Exception) {
            log.error(e) { "failure while notifying listener" }
            AsyncHttp2Stream.defaultStreamListener
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

    override fun onStreamOpened(stream: Stream) {
        super.onStreamOpened(stream)
        streamsOpened.incrementAndGet()
    }

    override fun onStreamClosed(stream: Stream) {
        super.onStreamClosed(stream)
        streamsClosed.incrementAndGet()
    }

    fun getStreamsOpened() = streamsOpened.get()

    fun getStreamsClosed() = streamsClosed.get()

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        val future = CompletableFuture<HttpClientResponse>()
        val contentProvider: HttpClientContentProvider? = request.contentProvider
        val contentHandler: HttpClientContentHandler? = request.contentHandler
        val metaDataRequest = toMetaDataRequest(request)
        requestChannel.offer(RequestMessage(metaDataRequest, contentProvider, contentHandler, future))
        return future
    }

    private data class RequestMessage(
        val request: MetaData.Request,
        val contentProvider: HttpClientContentProvider?,
        val contentHandler: HttpClientContentHandler?,
        val response: CompletableFuture<HttpClientResponse>
    )
}