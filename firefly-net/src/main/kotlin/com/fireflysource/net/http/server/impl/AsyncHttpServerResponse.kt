package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.server.HttpServerContentProvider
import com.fireflysource.net.http.server.HttpServerOutputChannel
import com.fireflysource.net.http.server.HttpServerResponse
import java.util.*
import java.util.function.Supplier

class AsyncHttpServerResponse : HttpServerResponse {

    val response: MetaData.Response = MetaData.Response(HttpVersion.HTTP_1_1, HttpStatus.OK_200, HttpFields())
    private var contentProvider: HttpServerContentProvider? = null
    private val cookieList: LinkedList<Cookie> by lazy { LinkedList<Cookie>() }

    override fun getStatus(): Int = response.status

    override fun setStatus(status: Int) {
        response.status = status
    }

    override fun getReason(): String = response.reason

    override fun setReason(reason: String) {
        response.reason = reason
    }

    override fun getHttpVersion(): HttpVersion = response.httpVersion

    override fun setHttpVersion(httpVersion: HttpVersion) {
        response.httpVersion = httpVersion
    }

    override fun getHttpFields(): HttpFields = response.fields

    override fun setHttpFields(httpFields: HttpFields) {
        response.fields.clear()
        response.fields.addAll(httpFields)
    }

    override fun getCookies(): List<Cookie> = cookieList

    override fun setCookies(cookies: List<Cookie>) {
        cookieList.clear()
        cookieList.addAll(cookies)
    }

    override fun getContentProvider(): HttpServerContentProvider? = contentProvider

    override fun setContentProvider(contentProvider: HttpServerContentProvider) {
        this.contentProvider = contentProvider
    }

    override fun getTrailerSupplier(): Supplier<HttpFields> = response.trailerSupplier

    override fun setTrailerSupplier(supplier: Supplier<HttpFields>) {
        response.trailerSupplier = supplier
    }


    override fun commit() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOutputChannel(): HttpServerOutputChannel {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}