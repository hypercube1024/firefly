package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientRequest
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
    private var expectServerAcceptsContent = false
    private var httpClientResponse: AsyncHttpClientResponse? = null
    private val trailers = HttpFields()
    private val responseChannel: Channel<AsyncHttpClientResponse> = Channel(Channel.UNLIMITED)
    private var isServerAcceptedContent: Boolean = false
    private var isHttpTunnel: Boolean = false
    private var httpRequest: HttpClientRequest? = null

    fun init(httpRequest: HttpClientRequest, expectServerAcceptsContent: Boolean, isHttpTunnel: Boolean) {
        this.httpRequest = httpRequest
        this.expectServerAcceptsContent = expectServerAcceptsContent
        this.isHttpTunnel = isHttpTunnel
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
                isServerAcceptedContent = true
            } else {
                isServerAcceptedContent = false
                updateResponseLine()
            }
            expectServerAcceptsContent = false
        } else updateResponseLine()
        return status == HttpStatus.CONTINUE_100
    }

    override fun parsedHeader(field: HttpField) {
        response.fields.add(field)
    }

    override fun headerComplete(): Boolean {
        val httpClientResponse = AsyncHttpClientResponse(MetaData.Response(response), httpRequest?.contentHandler)
        this.httpClientResponse = httpClientResponse
        return if (isHttpTunnel) {
            responseChannel.trySend(httpClientResponse)
            true
        } else {
            val request = httpRequest
            requireNotNull(request)
            request.headerComplete.accept(request, httpClientResponse)
            false
        }
    }

    override fun content(buffer: ByteBuffer): Boolean {
        httpRequest?.contentHandler?.accept(buffer, httpClientResponse)
        return false
    }

    override fun contentComplete(): Boolean {
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
        responseChannel.trySend(clientResponse)
        return true
    }

    override fun badMessage(failure: BadMessageException) {
        throw failure
    }

    override fun earlyEOF() {
        throw BadMessageException(HttpStatus.BAD_REQUEST_400)
    }

    suspend fun complete(): HttpClientResponse {
        val response = responseChannel.receive()
        httpRequest?.contentHandler?.closeAsync()?.await()
        return response
    }

    fun isServerAcceptedContent(): Boolean = isServerAcceptedContent

    fun reset() {
        response.recycle()
        httpRequest = null
        trailers.clear()
    }
}