package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import java.nio.ByteBuffer
import java.util.function.Supplier

class Http1ClientResponseHandler : HttpParser.ResponseHandler {

    private val response: MetaData.Response = MetaData.Response(HttpVersion.HTTP_1_1, 0, HttpFields())
    private var contentHandler: HttpClientContentHandler? = null
    private var expectServerAcceptsContent = false
    private var httpClientResponse: AsyncHttpClientResponse? = null
    private val trailers = HttpFields()
    private val responseChannel: Channel<AsyncHttpClientResponse> = Channel(Channel.UNLIMITED)
    private var serverAccepted: Boolean = false

    fun init(contentHandler: HttpClientContentHandler, expectServerAcceptsContent: Boolean) {
        this.contentHandler = contentHandler
        this.expectServerAcceptsContent = expectServerAcceptsContent
    }

    override fun getHeaderCacheSize(): Int {
        return 4096
    }

    override fun startResponse(version: HttpVersion, status: Int, reason: String): Boolean {
        fun updateResponseLine() {
            response.httpVersion = version
            response.status = status
            response.reason = reason
        }

        if (expectServerAcceptsContent) {
            if (status == HttpStatus.CONTINUE_100) {
                serverAccepted = true
            } else {
                serverAccepted = false
                updateResponseLine()
            }
            expectServerAcceptsContent = false
        } else updateResponseLine()
        return false
    }

    override fun parsedHeader(field: HttpField) {
        response.fields.add(field)
    }

    override fun headerComplete(): Boolean {
        httpClientResponse = AsyncHttpClientResponse(MetaData.Response(response), contentHandler)
        return false
    }

    override fun content(buffer: ByteBuffer): Boolean {
        contentHandler?.accept(buffer, httpClientResponse)
        return false
    }

    override fun contentComplete(): Boolean {
        contentHandler = null
        return false
    }

    override fun parsedTrailer(field: HttpField) {
        trailers.add(field)
    }

    override fun messageComplete(): Boolean {
        val clientResponse = httpClientResponse
        requireNotNull(clientResponse)
        val trailer = HttpFields(trailers)
        clientResponse.response.trailerSupplier = Supplier { trailer }
        responseChannel.offer(clientResponse)
        return true
    }

    override fun badMessage(failure: BadMessageException) {
        throw failure
    }

    override fun earlyEOF() {
        throw BadMessageException(HttpStatus.BAD_REQUEST_400)
    }

    suspend fun complete(): HttpClientResponse {
        contentHandler?.closeFuture()?.await()
        return responseChannel.receive()
    }

    fun serverAccepted(): Boolean = serverAccepted

    fun reset() {
        response.recycle()
        contentHandler = null
        trailers.clear()
    }
}