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

class Http1ClientResponseHandler : HttpParser.ResponseHandler {

    var response: MetaData.Response? = null
    var contentHandler: HttpClientContentHandler? = null
    var headerCompleted = false
    var httpClientResponse: AsyncHttpClientResponse? = null
    val trailers = HttpFields()

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
        headerCompleted = true
        val resp = MetaData.Response(
            response?.httpVersion, response?.status ?: 0, response?.reason,
            HttpFields(response?.fields), response?.contentLength ?: -1
        )
        httpClientResponse = AsyncHttpClientResponse(resp)
        return false
    }

    override fun content(buffer: ByteBuffer): Boolean {
        contentHandler?.accept(buffer, httpClientResponse)
        return false
    }

    override fun contentComplete(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun parsedTrailer(field: HttpField) {
        trailers.add(field)
    }

    override fun messageComplete(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun badMessage(failure: BadMessageException) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun earlyEOF() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun toHttpClientResponse(): HttpClientResponse {
        TODO("not implemented")
    }

    fun reset() {
        response?.recycle()
        contentHandler = null
        trailers.clear()
    }
}