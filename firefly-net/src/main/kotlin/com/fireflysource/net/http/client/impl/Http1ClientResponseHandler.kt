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
    var contentHandler: HttpClientContentHandler? = null
    var expect100Continue = false
    private var httpClientResponse: AsyncHttpClientResponse? = null
    private val trailers = HttpFields()
    private val responseChannel: Channel<AsyncHttpClientResponse> = Channel(Channel.UNLIMITED)
    private var expect100ContinueStatus: Int = 0

    override fun getHeaderCacheSize(): Int {
        return 4096
    }

    override fun startResponse(version: HttpVersion, status: Int, reason: String): Boolean {
        fun updateResponseLine() {
            response.httpVersion = version
            response.status = status
            response.reason = reason
        }

        if (expect100Continue) {
            if (status == HttpStatus.CONTINUE_100) {
                expect100ContinueStatus = status
            } else {
                expect100ContinueStatus = status
                updateResponseLine()
            }
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
        throw IllegalStateException("Early EOF")
    }

    suspend fun complete(): HttpClientResponse {
        contentHandler?.closeFuture()?.await()
        return responseChannel.receive()
    }

    fun getExpect100ContinueStatus(): Int = expect100ContinueStatus

    fun reset() {
        response.recycle()
        contentHandler = null
        trailers.clear()
    }
}