package com.fireflysource.net.http.client.impl

import com.fireflysource.common.`object`.Assert
import com.fireflysource.common.coroutine.launchGlobally
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.net.Connection
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientContentProvider
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
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
    private val tcpConnection: TcpConnection,
    requestHeaderBufferSize: Int = 4 * 1024,
    contentBufferSize: Int = 8 * 1024
) : Connection by tcpConnection, TcpCoroutineDispatcher by tcpConnection, HttpClientConnection {

    private val generator = HttpGenerator()
    private val handler = Http1ClientResponseHandler()
    private val parser = HttpParser(handler)

    private val outChannel = Channel<RequestMessage>(Channel.UNLIMITED)
    private val headerBuffer = BufferUtils.allocateDirect(requestHeaderBufferSize)
    private val contentBuffer = BufferUtils.allocateDirect(contentBufferSize)
    private val chunkBuffer = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE)


    init {
        val acceptRequestJob = launchGlobally(tcpConnection.coroutineDispatcher) {
            recvLoop@ while (true) {
                val message = outChannel.receive()

                try {
                    encodeRequestAndFlushData(message)
                    message.response.complete(parseResponse())
                } catch (e: Exception) {
                    message.response.completeExceptionally(e)
                }
            }
        }
        tcpConnection.onClose {
            outChannel.close()
            acceptRequestJob.cancel()
        }
    }

    private suspend fun parseResponse(): HttpClientResponse {
        TODO("")
    }

    private suspend fun encodeRequestAndFlushData(message: RequestMessage) {
        genLoop@ while (true) {
            when (generator.state) {
                START -> {
                    val hasContent = (message.content != null)
                    val result =
                        generator.generateRequest(message.request, headerBuffer, null, null, !hasContent)
                    Assert.state(result == FLUSH, "The HTTP client generator result error. $result")
                    flushHeaderBuffer()
                }
                COMMITTED -> {
                    requireNotNull(message.content)
                    val pos = BufferUtils.flipToFill(contentBuffer)
                    val len = message.content.read(contentBuffer).await()
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
                    message.content?.close()
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
        val future = CompletableFuture<HttpClientResponse>()
        val metaDataRequest = toMetaDataRequest(request)
        outChannel.offer(RequestMessage(metaDataRequest, request.contentProvider, future))
        return future
    }

    private data class RequestMessage(
        val request: MetaData.Request,
        val content: HttpClientContentProvider?,
        val response: CompletableFuture<HttpClientResponse>
    )
}