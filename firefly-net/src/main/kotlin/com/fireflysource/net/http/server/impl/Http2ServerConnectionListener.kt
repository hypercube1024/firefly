package com.fireflysource.net.http.server.impl

import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.common.v2.frame.*
import com.fireflysource.net.http.common.v2.stream.Http2Connection
import com.fireflysource.net.http.common.v2.stream.Stream
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import java.util.function.Consumer

class Http2ServerConnectionListener(private val dispatcher: TcpCoroutineDispatcher) :
    Http2Connection.Listener.Adapter() {

    companion object {
        private val log = SystemLogger.create(Http2ServerConnectionListener::class.java)
    }

    var connectionListener: HttpServerConnection.Listener = HttpServerConnection.EMPTY_LISTENER

    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
        val http2Connection = stream.http2Connection as Http2ServerConnection

        val request = AsyncHttpServerRequest(frame.metaData as MetaData.Request)
        val response = Http2ServerResponse(http2Connection, stream)
        val context = AsyncRoutingContext(request, response, http2Connection)
        val trailer: HttpFields by lazy { HttpFields() }
        var receivedData = false

        val headerComplete = connectionListener.onHeaderComplete(context)
        if (frame.isEndStream) {
            headerComplete.thenCompose { connectionListener.onHttpRequestComplete(context) }
        }

        return object : Stream.Listener.Adapter() {
            override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                log.debug { "HTTP2 server received trailer frame. id: ${stream.id}" }
                if (receivedData) {
                    trailer.addAll(frame.metaData.fields)
                }
                if (frame.isEndStream) {
                    headerComplete.thenCompose { connectionListener.onHttpRequestComplete(context) }
                }
            }

            override fun onData(stream: Stream, frame: DataFrame, result: Consumer<Result<Void>>) {
                receivedData = true
                try {
                    context.request.contentHandler.accept(frame.data, context)
                    log.debug { "HTTP2 server accepts content success. id: ${stream.id}" }
                    result.accept(Result.SUCCESS)
                } catch (e: Exception) {
                    log.error(e) { "HTTP2 server accepts content exception. id: ${stream.id}" }
                    result.accept(Result.createFailedResult(e))
                }
                if (frame.isEndStream) {
                    headerComplete
                        .thenCompose { context.request.contentHandler.closeFuture() }
                        .thenCompose {
                            context.request.isRequestComplete = true
                            connectionListener.onHttpRequestComplete(context)
                        }
                }
            }

            override fun onClosed(stream: Stream) {
                log.debug { "HTTP2 server stream closed. id: ${stream.id}" }
            }

            override fun onReset(stream: Stream, frame: ResetFrame, result: Consumer<Result<Void>>) {
                val e = IllegalStateException(ErrorCode.toString(frame.error, "stream reset. id: ${stream.id}"))
                connectionListener.onException(context, e)
                    .thenAccept { result.accept(Result.SUCCESS) }
                    .exceptionally {
                        result.accept(Result.createFailedResult(it))
                        null
                    }
            }

            override fun onFailure(stream: Stream, error: Int, reason: String, result: Consumer<Result<Void>>) {
                val defaultError = "stream failure. id: ${stream.id}, reason: $reason"
                val e = IllegalStateException(ErrorCode.toString(error, defaultError))
                connectionListener.onException(context, e)
                    .thenAccept { result.accept(Result.SUCCESS) }
                    .exceptionally {
                        result.accept(Result.createFailedResult(it))
                        null
                    }
            }

        }
    }

    override fun onClose(http2Connection: Http2Connection, frame: GoAwayFrame) {
        log.info { "HTTP2 server connection closed. id: ${http2Connection.id}, frame: $frame" }
    }

    override fun onFailure(http2Connection: Http2Connection, failure: Throwable) {
        connectionListener.onException(null, failure)
    }

    override fun onReset(http2Connection: Http2Connection, frame: ResetFrame) {
        val e = IllegalStateException(ErrorCode.toString(frame.error, "stream exception"))
        connectionListener.onException(null, e)
    }
}

sealed class Http2StreamInputMessage

data class HeadersInputMessage(val frame: HeadersFrame) : Http2StreamInputMessage()

data class DataInputMessage(val frame: DataFrame, val result: Consumer<Result<Void>>) : Http2StreamInputMessage()

data class TrailerInputMessage(val frame: HeadersFrame) : Http2StreamInputMessage()

object StreamShutdown : Http2StreamInputMessage()