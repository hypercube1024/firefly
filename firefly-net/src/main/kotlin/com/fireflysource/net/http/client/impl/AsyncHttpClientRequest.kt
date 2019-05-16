package com.fireflysource.net.http.client.impl

import com.fireflysource.common.func.Callback
import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientContentProvider
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.codec.UrlEncoded
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.Cookie
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.common.io.OutputChannel
import java.util.function.Consumer
import java.util.function.Supplier

class AsyncHttpClientRequest : HttpClientRequest {

    companion object {
        private val defaultHttpUri = HttpURI("http://localhost:8080/")
        private val defaultMethod = HttpMethod.GET.value
    }

    private var method: String = defaultMethod
    private var uri: HttpURI = defaultHttpUri
    private var queryParameters: UrlEncoded? = null
    private var httpFields: HttpFields = HttpFields()
    private var cookies: MutableList<Cookie>? = null
    private var trailerSupplier: Supplier<HttpFields>? = null
    private var contentProvider: HttpClientContentProvider? = null
    private var outputChannel: Consumer<OutputChannel>? = null
    private var headerComplete: Consumer<HttpClientResponse>? = null
    private var contentHandler: HttpClientContentHandler? = null
    private var contentComplete: Consumer<HttpClientResponse>? = null
    private var messageComplete: Consumer<HttpClientResponse>? = null
    private var badMessage: Consumer<BadMessageException>? = null
    private var earlyEof: Callback? = null
    private var http2Settings: Map<Int, Int>? = null

    override fun getMethod(): String = method

    override fun setMethod(method: String) {
        this.method = method
    }

    override fun getURI(): HttpURI = uri

    override fun setURI(uri: HttpURI) {
        this.uri = uri
    }

    override fun getQueryParameters(): UrlEncoded {
        if (this.queryParameters == null) {
            this.queryParameters = UrlEncoded()
        }
        return this.queryParameters!!
    }

    override fun setQueryParameters(queryParameters: UrlEncoded) {
        this.queryParameters = queryParameters
    }

    override fun getHttpFields(): HttpFields = httpFields

    override fun setHttpFields(httpFields: HttpFields) {
        this.httpFields = httpFields
    }

    override fun getCookies(): MutableList<Cookie>? = cookies

    override fun setCookies(cookies: MutableList<Cookie>?) {
        this.cookies = cookies
    }

    override fun getTrailerSupplier(): Supplier<HttpFields>? = trailerSupplier

    override fun setTrailerSupplier(trailerSupplier: Supplier<HttpFields>?) {
        this.trailerSupplier = trailerSupplier
    }

    override fun setContentProvider(contentProvider: HttpClientContentProvider?) {
        this.contentProvider = contentProvider
    }

    override fun getContentProvider(): HttpClientContentProvider? = contentProvider

    override fun getOutputChannel(): Consumer<OutputChannel>? = outputChannel

    override fun setOutputChannel(outputChannel: Consumer<OutputChannel>?) {
        this.outputChannel = outputChannel
    }

    override fun setHeaderComplete(headerComplete: Consumer<HttpClientResponse>?) {
        this.headerComplete = headerComplete
    }

    override fun getHeaderComplete(): Consumer<HttpClientResponse>? = headerComplete

    override fun setContentHandler(contentHandler: HttpClientContentHandler?) {
        this.contentHandler = contentHandler
    }

    override fun getContentHandler(): HttpClientContentHandler? = contentHandler

    override fun setContentComplete(contentComplete: Consumer<HttpClientResponse>?) {
        this.contentComplete = contentComplete
    }

    override fun getContentComplete(): Consumer<HttpClientResponse>? = contentComplete

    override fun setMessageComplete(messageComplete: Consumer<HttpClientResponse>?) {
        this.messageComplete = messageComplete
    }

    override fun getMessageComplete(): Consumer<HttpClientResponse>? = messageComplete

    override fun setBadMessage(badMessage: Consumer<BadMessageException>?) {
        this.badMessage = badMessage
    }

    override fun getBadMessage(): Consumer<BadMessageException>? = badMessage

    override fun setEarlyEof(earlyEof: Callback?) {
        this.earlyEof = earlyEof
    }

    override fun getEarlyEof(): Callback? = earlyEof

    override fun setHttp2Settings(http2Settings: Map<Int, Int>?) {
        this.http2Settings = http2Settings
    }

    override fun getHttp2Settings(): Map<Int, Int>? = http2Settings
}