package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.HttpField
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import java.nio.ByteBuffer
import java.util.function.Supplier

class Http1ClientResponseHandler : HttpParser.ResponseHandler {

    private var response: MetaData.Response? = null
    var contentHandler: HttpClientContentHandler? = null
    private var httpClientResponse: AsyncHttpClientResponse? = null
    private val trailers = HttpFields()

    override fun getHeaderCacheSize(): Int {
        return 4096
    }

    override fun startResponse(version: HttpVersion, status: Int, reason: String): Boolean {
        response = MetaData.Response(version, status, reason, HttpFields(), -1)
        return false
    }

    override fun parsedHeader(field: HttpField) {
        response?.fields?.add(field)
    }

    override fun headerComplete(): Boolean {
        val resp = MetaData.Response(
            response?.httpVersion, response?.status ?: 0, response?.reason,
            HttpFields(response?.fields), response?.contentLength ?: -1
        )
        httpClientResponse = AsyncHttpClientResponse(resp, contentHandler)
        return false
    }

    override fun content(buffer: ByteBuffer): Boolean {
        contentHandler?.accept(buffer, httpClientResponse)
        return false
    }

    override fun contentComplete(): Boolean {
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

    fun toHttpClientResponse(): HttpClientResponse {
        return httpClientResponse!!
    }

    fun reset() {
        response?.recycle()
        contentHandler = null
        trailers.clear()
    }
}