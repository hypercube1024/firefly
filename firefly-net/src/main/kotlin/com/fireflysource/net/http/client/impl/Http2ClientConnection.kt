package com.fireflysource.net.http.client.impl

import com.fireflysource.common.concurrent.exceptionallyAccept
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
import com.fireflysource.net.tcp.aio.AdaptiveBufferSize
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.UnaryOperator

class Http2ClientConnection(
    config: HttpConfig,
    tcpConnection: TcpConnection,
    flowControl: FlowControl = SimpleFlowControlStrategy(),
    listener: Http2Connection.Listener = defaultHttp2ConnectionListener
) : AsyncHttp2Connection(1, config, tcpConnection, flowControl, listener), HttpClientConnection {

    companion object {
        private val log = SystemLogger.create(Http2ClientConnection::class.java)
        private val defaultServerAccepted: CompletableFuture<Boolean> by lazy {
            val future = CompletableFuture<Boolean>()
            future.complete(true)
            future
        }
    }

    private val parser: Parser = Parser(this, config.maxDynamicTableSize, config.maxHeaderSize)
    private val adaptiveBufferSize = AdaptiveBufferSize()

    init {
        parser.init(UnaryOperator.identity())
        sendConnectionPreface()
        launchParserJob(parser)
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
                .thenAccept { log.info { "send connection preface success. id: $id" } }
                .exceptionallyAccept { log.error(it) { "send connection preface exception. id: $id" } }
        } else {
            sendControlFrame(null, prefaceFrame, settingsFrame)
                .thenAccept { log.info { "send connection preface success. id: $id" } }
                .exceptionallyAccept { log.error(it) { "send connection preface exception. id: $id" } }
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

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        val future = CompletableFuture<HttpClientResponse>()
        val metaDataRequest: MetaData.Request = toMetaDataRequest(request)

        val contentProvider: HttpClientContentProvider? = request.contentProvider
        val contentHandler: HttpClientContentHandler? = request.contentHandler
        val lastHeaders = contentProvider == null && metaDataRequest.trailerSupplier == null
        val headersFrame = HeadersFrame(metaDataRequest, null, lastHeaders)

        val response = AsyncHttpClientResponse(MetaData.Response(HttpVersion.HTTP_2, 0, HttpFields()), contentHandler)
        val metaDataResponse = response.response

        val expectServerAcceptsContent = request.httpFields.expectServerAcceptsContent()
        val serverAccepted = if (expectServerAcceptsContent) CompletableFuture() else defaultServerAccepted
        val trailer = HttpFields()
        var theFirstHeader = true
        var receivedData = false

        val streamListener = object : Stream.Listener.Adapter() {

            fun onMessageComplete() {
                if (contentHandler != null)
                    contentHandler.closeFuture().thenAccept { future.complete(response) }
                else
                    future.complete(response)
            }

            fun copyResponse(frame: HeadersFrame) {
                when (val metaData = frame.metaData) {
                    is MetaData.Response -> {
                        metaDataResponse.status = metaData.status
                        metaDataResponse.reason = metaData.reason
                        metaDataResponse.fields.addAll(metaData.fields)
                    }
                    is MetaData.Request -> {
                    }
                    else -> {
                        if (receivedData) {
                            if (metaDataResponse.trailerSupplier == null) {
                                metaDataResponse.setTrailerSupplier { trailer }
                            }
                            trailer.addAll(metaData.fields)
                        } else metaDataResponse.fields.addAll(metaData.fields)
                    }
                }
                if (frame.isEndStream) onMessageComplete()
            }

            override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                if (theFirstHeader) {
                    theFirstHeader = false
                    val metaData = frame.metaData
                    if (metaData is MetaData.Response) {
                        if (expectServerAcceptsContent) {
                            if (metaData.status == HttpStatus.CONTINUE_100) {
                                log.debug { "Client received 100 continue response. stream: $stream" }
                                serverAccepted.complete(true)
                            } else {
                                serverAccepted.complete(false)
                                copyResponse(frame)
                            }
                        } else {
                            serverAccepted.complete(true)
                            copyResponse(frame)
                        }
                    } else {
                        serverAccepted.complete(false)
                        val resetFrame = ResetFrame(stream.id, ErrorCode.INTERNAL_ERROR.code)
                        stream.reset(resetFrame) {
                            val exception =
                                IllegalStateException("The HTTP2 client must be not receive request meta data.")
                            future.completeExceptionally(exception)
                        }
                    }
                } else {
                    copyResponse(frame)
                }
            }

            override fun onData(stream: Stream, frame: DataFrame, result: Consumer<Result<Void>>) {
                try {
                    receivedData = true
                    contentHandler?.accept(frame.data, response)
                    if (frame.isEndStream) onMessageComplete()
                } finally {
                    result.accept(Result.SUCCESS)
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
        }
        newStream(headersFrame, streamListener)
            .thenCompose { newStream -> serverAccepted.thenApply { Pair(newStream, it) } }
            .thenCompose { generateContent(contentProvider, metaDataRequest, it.first, it.second) }
            .thenAccept { generateTrailer(metaDataRequest, it.first, it.second) }
            .exceptionallyAccept {
                log.error(it) { "The HTTP2 client connection creates local stream failure. id: $id " }
                future.completeExceptionally(it)
            }
        return future
    }

    private fun generateContent(
        contentProvider: HttpClientContentProvider?,
        metaDataRequest: MetaData.Request,
        newStream: Stream,
        serverAccept: Boolean
    ) = tcpConnection.coroutineScope.async {
        if (contentProvider != null && serverAccept) {
            val byteBuffers = LinkedList<ByteBuffer>()
            readLoop@ while (true) {
                val contentBuffer = BufferUtils.allocate(adaptiveBufferSize.getBufferSize())
                val pos = contentBuffer.flipToFill()
                val length = contentProvider.read(contentBuffer).await()
                contentBuffer.flipToFlush(pos)
                adaptiveBufferSize.update(length)

                when {
                    length > 0 -> byteBuffers.offer(contentBuffer)
                    length < 0 -> break@readLoop
                }

                if (byteBuffers.size > 1) {
                    val dataFrame = DataFrame(newStream.id, byteBuffers.poll(), false)
                    newStream.data(dataFrame)
                }
            }
            val last = metaDataRequest.trailerSupplier == null
            val dataFrame = DataFrame(newStream.id, byteBuffers.poll(), last)
            newStream.data(dataFrame)
            contentProvider.closeFuture()
        }
        Pair(newStream, serverAccept)
    }.asCompletableFuture()

    private fun generateTrailer(metaDataRequest: MetaData.Request, newStream: Stream, serverAccept: Boolean) {
        val trailerSupplier = metaDataRequest.trailerSupplier
        if (trailerSupplier != null && serverAccept) {
            val trailerMetaData = MetaData.Request(trailerSupplier.get())
            trailerMetaData.isOnlyTrailer = true
            val headersFrameTrailer = HeadersFrame(newStream.id, trailerMetaData, null, true)
            newStream.headers(headersFrameTrailer, discard())
        }
    }
}