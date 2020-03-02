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
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.function.Consumer

class Http2ServerConnectionListener(private val dispatcher: TcpCoroutineDispatcher) :
    Http2Connection.Listener.Adapter() {

    companion object {
        private val log = SystemLogger.create(Http2ServerConnectionListener::class.java)
    }

    var connectionListener: HttpServerConnection.Listener = HttpServerConnection.EMPTY_LISTENER

    private inner class Http2StreamHandler(
        private val scope: CoroutineScope,
        private val context: AsyncRoutingContext
    ) {

        private val streamMessageChannel: Channel<Http2StreamInputMessage> = Channel(Channel.UNLIMITED)
        private val trailer: HttpFields by lazy { HttpFields() }
        private var receivedData = false

        init {
            streamMessageJob()
        }

        fun streamMessageJob() = scope.launch {
            messageLoop@ while (true) {
                try {
                    when (val message = streamMessageChannel.receive()) {
                        is HeadersInputMessage -> handleHeaders(message)
                        is DataInputMessage -> handleData(message)
                        is StreamShutdown -> break@messageLoop
                    }
                } catch (e: Throwable) {
                    notifyException(e)
                    break@messageLoop
                }
            }
        }

        fun sendMessage(message: Http2StreamInputMessage) {
            streamMessageChannel.offer(message)
        }

        suspend fun handleHeaders(message: HeadersInputMessage) {
            val frame = message.frame
            if (receivedData) {
                trailer.addAll(frame.metaData.fields)
                if (frame.isEndStream) {
                    val asyncRequest = context.request as AsyncHttpServerRequest
                    asyncRequest.request.setTrailerSupplier { trailer }
                    notifyRequestComplete()
                }
            } else {
                context.request.httpFields.addAll(frame.metaData.fields)
                if (frame.isEndStream) {
                    notifyHeaderComplete()
                    notifyRequestComplete()
                }
            }
        }

        suspend fun handleData(message: DataInputMessage) {
            if (!receivedData) {
                notifyHeaderComplete()
                receivedData = true
            }
            acceptContent(message)
            if (message.frame.isEndStream) {
                context.request.contentHandler.closeFuture().await()
                notifyRequestComplete()
            }
        }

        suspend fun notifyHeaderComplete() {
            try {
                connectionListener.onHeaderComplete(context).await()
            } catch (e: Exception) {
                log.error(e) { "HTTP2 server on headers complete exception. id: ${scope.coroutineContext[CoroutineName]}" }
            }
        }

        private fun acceptContent(message: DataInputMessage) {
            val (frame, result) = message
            try {
                context.request.contentHandler.accept(frame.data, context)
                log.debug { "HTTP2 server accepts content success. id: ${scope.coroutineContext[CoroutineName]}" }
                result.accept(Result.SUCCESS)
            } catch (e: Exception) {
                log.error(e) { "HTTP2 server accepts content exception. id:  ${scope.coroutineContext[CoroutineName]}" }
                result.accept(Result.createFailedResult(e))
            }
        }

        suspend fun notifyRequestComplete() {
            try {
                connectionListener.onHttpRequestComplete(context).await()
            } catch (e: Exception) {
                log.error(e) { "HTTP2 server on request complete exception. id: ${scope.coroutineContext[CoroutineName]}" }
            }
        }

        private suspend fun notifyException(exception: Throwable) {
            try {
                log.error(exception) { "HTTP2 server on stream message exception. id: ${scope.coroutineContext[CoroutineName]}" }
                connectionListener.onException(context, exception).await()
            } catch (e: Exception) {
                log.error(e) { "HTTP2 server on stream message exception. id: ${scope.coroutineContext[CoroutineName]}" }
            }
        }

    }

    override fun onNewStream(stream: Stream, frame: HeadersFrame): Stream.Listener {
        val http2Connection = stream.http2Connection as Http2ServerConnection
        val streamScope = newCoroutineScope(stream)
        val request = AsyncHttpServerRequest(frame.metaData as MetaData.Request)
        val response = Http2ServerResponse(http2Connection, stream)
        val context = AsyncRoutingContext(request, response, http2Connection)
        val handler = Http2StreamHandler(streamScope, context)
        handler.sendMessage(HeadersInputMessage(frame))

        return object : Stream.Listener.Adapter() {

            override fun onHeaders(stream: Stream, frame: HeadersFrame) {
                handler.sendMessage(HeadersInputMessage(frame))
            }

            override fun onData(stream: Stream, frame: DataFrame, result: Consumer<Result<Void>>) {
                handler.sendMessage(DataInputMessage(frame, result))
            }

            override fun onClosed(stream: Stream) {
                handler.sendMessage(StreamShutdown)
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

    private fun newCoroutineScope(stream: Stream): CoroutineScope {
        return CoroutineScope(
            dispatcher.coroutineScope.coroutineContext +
                    SupervisorJob(dispatcher.supervisorJob) +
                    CoroutineName("StreamScope#${stream.id}")
        )
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

object StreamShutdown : Http2StreamInputMessage()