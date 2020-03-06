package com.fireflysource.net.http.client.impl

import com.fireflysource.common.concurrent.exceptionallyAccept
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.common.v2.frame.DataFrame
import com.fireflysource.net.http.common.v2.frame.ErrorCode
import com.fireflysource.net.http.common.v2.frame.HeadersFrame
import com.fireflysource.net.http.common.v2.frame.ResetFrame
import com.fireflysource.net.http.common.v2.stream.Stream
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

class Http2ClientStreamListener(
    request: HttpClientRequest,
    private val future: CompletableFuture<HttpClientResponse>
) : Stream.Listener.Adapter() {

    companion object {
        private val log = SystemLogger.create(Http2ClientStreamListener::class.java)
        private val defaultServerAccepted: CompletableFuture<Boolean> by lazy {
            val future = CompletableFuture<Boolean>()
            future.complete(true)
            future
        }
    }

    private val contentHandler: HttpClientContentHandler? = request.contentHandler
    private val expectServerAcceptsContent: Boolean = request.httpFields.expectServerAcceptsContent()

    private val response =
        AsyncHttpClientResponse(MetaData.Response(HttpVersion.HTTP_2, 0, HttpFields()), contentHandler)
    private val metaDataResponse = response.response
    val serverAccepted = if (expectServerAcceptsContent) CompletableFuture() else defaultServerAccepted
    private val trailer = HttpFields()
    private var theFirstHeader = true
    private var receivedData = false

    private fun onMessageComplete() {
        if (contentHandler != null) {
            contentHandler.closeFuture()
                .thenAccept { future.complete(response) }
                .exceptionallyAccept { future.completeExceptionally(it) }
        } else future.complete(response)
    }

    private fun handleResponseHeaders(stream: Stream, frame: HeadersFrame) {
        when (val metaData = frame.metaData) {
            is MetaData.Response -> {
                metaDataResponse.status = metaData.status
                metaDataResponse.reason = metaData.reason
                metaDataResponse.fields.addAll(metaData.fields)
            }
            is MetaData.Request -> handleError(stream, "The HTTP2 client must receive response metadata.")
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

    private fun handleError(stream: Stream, message: String) {
        val resetFrame = ResetFrame(stream.id, ErrorCode.INTERNAL_ERROR.code)
        stream.reset(resetFrame) {
            val exception = IllegalStateException(message)
            future.completeExceptionally(exception)
        }
    }

    override fun onHeaders(stream: Stream, frame: HeadersFrame) {
        if (theFirstHeader) {
            theFirstHeader = false
            val metaData = frame.metaData
            if (metaData is MetaData.Response) {
                if (expectServerAcceptsContent) {
                    if (metaData.status == HttpStatus.CONTINUE_100) {
                        log.debug { "Client received 100 continue response. stream: $stream" }
                        if (frame.isEndStream) {
                            serverAccepted.complete(false)
                            handleError(stream, "The remote stream closed. id: ${stream.id}")
                        } else serverAccepted.complete(true)
                    } else {
                        serverAccepted.complete(false)
                        handleResponseHeaders(stream, frame)
                    }
                } else {
                    serverAccepted.complete(true)
                    handleResponseHeaders(stream, frame)
                }
            } else {
                serverAccepted.complete(false)
                handleError(stream, "The HTTP2 client must receive response metadata.")
            }
        } else {
            handleResponseHeaders(stream, frame)
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