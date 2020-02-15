package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.HttpField
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import kotlinx.coroutines.future.await
import java.nio.ByteBuffer
import java.util.function.Supplier

class Http1ClientResponseHandler : HttpParser.ResponseHandler {

    private val response: MetaData.Response = MetaData.Response(HttpVersion.HTTP_1_1, 0, HttpFields())
    var contentHandler: HttpClientContentHandler? = null
    private var httpClientResponse: AsyncHttpClientResponse? = null
    private val trailers = HttpFields()

    override fun getHeaderCacheSize(): Int {
        return 4096
    }

    override fun startResponse(version: HttpVersion, status: Int, reason: String): Boolean {
        response.httpVersion = version
        response.status = status
        response.reason = reason
        return false
    }

    override fun parsedHeader(field: HttpField) {
        response.fields.add(field)
    }

    override fun headerComplete(): Boolean {
        val resp = MetaData.Response(
            response.httpVersion, response.status, response.reason,
            HttpFields(response.fields), response.contentLength
        )
        httpClientResponse = AsyncHttpClientResponse(resp, contentHandler)
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
        httpClientResponse?.response?.trailerSupplier = Supplier { HttpFields(trailers) }
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
        return httpClientResponse ?: throw IllegalStateException("Not received response.")
    }

    fun reset() {
        response.recycle()
        contentHandler = null
        trailers.clear()
    }
}