package com.fireflysource.net.http.client.impl

import com.fireflysource.common.`object`.Assert
import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.Connection
import com.fireflysource.net.http.client.*
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpHeaderValue
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator.Result.*
import com.fireflysource.net.http.common.v1.encoder.HttpGenerator.State.*
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

class Http1ClientConnection(
    config: HttpClientConfig,
    val tcpConnection: TcpConnection
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, HttpClientConnection {

    companion object {
        private val log = SystemLogger.create(Http1ClientConnection::class.java)
    }

    private val generator = HttpGenerator()

    // generator buffer
    private val headerBuffer = BufferUtils.allocateDirect(config.requestHeaderBufferSize)
    private val contentBuffer = BufferUtils.allocateDirect(config.contentBufferSize)
    private val chunkBuffer = BufferUtils.allocateDirect(HttpGenerator.CHUNK_SIZE)

    // parser
    private val handler = Http1ClientResponseHandler()
    private val parser = HttpParser(handler)

    private val requestChannel = Channel<RequestMessage>(Channel.UNLIMITED)
    private val acceptRequestJob = launchGlobally(tcpConnection.coroutineDispatcher) {
        while (true) {
            val requestMessage = requestChannel.receive()

            try {
                encodeRequestAndFlushData(requestMessage)

                // receive response data
                handler.contentHandler = requestMessage.contentHandler
                val response = parseResponse()
                requestMessage.response.complete(response)
            } catch (e: Exception) {
                requestMessage.response.completeExceptionally(e)
            } finally {
                handler.reset()
                parser.reset()
                generator.reset()
            }
        }
    }


    init {
        tcpConnection.onClose {
            acceptRequestJob.cancel()
        }
    }

    private suspend fun parseResponse(): HttpClientResponse {
        Assert.state(parser.isState(HttpParser.State.START), "The parser state error. ${parser.state}")

        val inputChannel = tcpConnection.inputChannel
        recvLoop@ while (!parser.isState(HttpParser.State.END)) {
            val buffer = inputChannel.receive()

            var remaining = buffer.remaining()
            readBufLoop@ while (!parser.isState(HttpParser.State.END) && remaining > 0) {
                val wasRemaining = remaining
                parser.parseNext(buffer)
                remaining = buffer.remaining()
                Assert.state(remaining != wasRemaining, "The received data can not be consumed")
            }
        }

        return handler.toHttpClientResponse()
    }

    private suspend fun encodeRequestAndFlushData(requestMessage: RequestMessage) {
        genLoop@ while (true) {
            when (generator.state) {
                START -> {
                    val hasContent = (requestMessage.contentProvider != null)
                    val result =
                        generator.generateRequest(requestMessage.request, headerBuffer, null, null, !hasContent)
                    Assert.state(result == FLUSH, "The HTTP client generator result error. $result")
                    flushHeaderBuffer()
                }
                COMMITTED -> {
                    requireNotNull(requestMessage.contentProvider)
                    val pos = BufferUtils.flipToFill(contentBuffer)
                    val len = requestMessage.contentProvider.read(contentBuffer).await()
                    BufferUtils.flipToFlush(contentBuffer, pos)

                    val last = (len == -1)

                    if (generator.isChunking) {
                        when (val result =
                            generator.generateRequest(null, null, chunkBuffer, contentBuffer, last)) {
                            FLUSH -> flushChunkedContentBuffer()
                            CONTINUE -> {
                            }
                            else -> throw IllegalStateException("The HTTP client generator result error. $result")
                        }
                    } else {
                        val result = generator.generateRequest(null, null, null, contentBuffer, last)
                        Assert.state(result == FLUSH, "The HTTP client generator result error. $result")
                        flushContentBuffer()
                    }
                }
                COMPLETING -> {
                    if (generator.isChunking) {
                        when (val result = generator.generateRequest(null, null, chunkBuffer, null, true)) {
                            FLUSH -> flushChunkBuffer()
                            DONE -> {
                            }
                            else -> throw IllegalStateException("The HTTP client generator result error. $result")
                        }
                    } else {
                        val result = generator.generateRequest(null, null, null, null, true)
                        Assert.state(result == DONE, "The HTTP client generator result error. $result")
                    }
                }
                END -> {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    requestMessage.contentProvider?.close()
                    break@genLoop
                }
                else -> throw IllegalStateException("The HTTP client generator state error. ${generator.state}")
            }
        }
    }

    private suspend fun flushHeaderBuffer() {
        if (headerBuffer.hasRemaining()) {
            tcpConnection.write(headerBuffer).await()
        }
        BufferUtils.clear(headerBuffer)
    }

    private suspend fun flushContentBuffer() {
        if (contentBuffer.hasRemaining()) {
            tcpConnection.write(contentBuffer).await()
        }
        BufferUtils.clear(contentBuffer)
    }

    private suspend fun flushChunkedContentBuffer() {
        val bufArray = arrayOf(chunkBuffer, contentBuffer)
        val remaining = bufArray.map { it.remaining().toLong() }.sum()
        if (remaining > 0) {
            tcpConnection.write(bufArray, 0, bufArray.size).await()
        }
        bufArray.forEach(BufferUtils::clear)
    }

    private suspend fun flushChunkBuffer() {
        if (chunkBuffer.hasRemaining()) {
            tcpConnection.write(chunkBuffer).await()
        }
        BufferUtils.clear(chunkBuffer)
    }

    override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_1_1

    override fun isSecureConnection(): Boolean = tcpConnection.isSecureConnection

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        prepareHttp1Headers(request)

        val future = CompletableFuture<HttpClientResponse>()
        val metaDataRequest = toMetaDataRequest(request)
        requestChannel.offer(RequestMessage(metaDataRequest, request.contentProvider, request.contentHandler, future))
        return future
    }
}

private data class RequestMessage(
    val request: MetaData.Request,
    val contentProvider: HttpClientContentProvider?,
    val contentHandler: HttpClientContentHandler?,
    val response: CompletableFuture<HttpClientResponse>
)

fun prepareHttp1Headers(request: HttpClientRequest) {
    if (request.httpFields.getValuesList(HttpHeader.HOST.value).isEmpty()) {
        request.httpFields.put(HttpHeader.HOST, request.uri.host)
    }

    val connectionValues = request.httpFields.getCSV(HttpHeader.CONNECTION, false)
    if (connectionValues.isNotEmpty()) {
        if (!connectionValues.contains(HttpHeaderValue.KEEP_ALIVE.value)) {
            val newValues = mutableListOf<String>()
            newValues.addAll(connectionValues)
            newValues.add(HttpHeaderValue.KEEP_ALIVE.value)
            request.httpFields.remove(HttpHeader.CONNECTION)
            request.httpFields.addCSV(HttpHeader.CONNECTION, *newValues.toTypedArray())
        }
    } else {
        request.httpFields.put(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE.value)
    }
}