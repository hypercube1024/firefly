package com.fireflysource.net.http.client.impl

import com.fireflysource.common.func.Callback
import com.fireflysource.common.io.OutputChannel
import com.fireflysource.common.string.StringUtils
import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientContentProvider
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.client.impl.content.provider.MultiPartContentProvider
import com.fireflysource.net.http.client.impl.content.provider.StringContentProvider
import com.fireflysource.net.http.common.codec.CookieGenerator
import com.fireflysource.net.http.common.codec.UrlEncoded
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.*
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import java.util.function.Supplier

class AsyncHttpClientRequest : HttpClientRequest {

    companion object {
        private val defaultHttpUri = HttpURI("http://localhost:8080/")
        private val defaultMethod = HttpMethod.GET.value
    }

    private var method: String = defaultMethod
    private var uri: HttpURI = defaultHttpUri
    private var httpVersion: HttpVersion = HttpVersion.HTTP_1_1
    private var queryParameters: UrlEncoded? = null
    private var formParameters: UrlEncoded? = null
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

    override fun getHttpVersion(): HttpVersion = httpVersion

    override fun setHttpVersion(httpVersion: HttpVersion) {
        this.httpVersion = httpVersion
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

    override fun getFormParameters(): UrlEncoded {
        if (this.formParameters == null) {
            this.formParameters = UrlEncoded()
        }
        return this.formParameters!!
    }

    override fun setFormParameters(formParameters: UrlEncoded?) {
        this.formParameters = formParameters
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

fun toMetaDataRequest(request: HttpClientRequest): MetaData.Request {
    if (request.formParameters.size > 0) {
        val formStr = request.formParameters.encode(StandardCharsets.UTF_8, true)
        val stringContentProvider = StringContentProvider(formStr, StandardCharsets.UTF_8)
        request.contentProvider = stringContentProvider
        request.httpFields.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.FORM_ENCODED.value)
        request.httpFields.put(HttpHeader.CONTENT_LENGTH, stringContentProvider.length().toString())
    } else {
        if (request.contentProvider != null) {
            if (request.contentProvider is MultiPartContentProvider) {
                val multiPartContentProvider = (request.contentProvider as MultiPartContentProvider)
                request.httpFields.put(HttpHeader.CONTENT_TYPE, multiPartContentProvider.contentType)
                if (multiPartContentProvider.length() >= 0) {
                    request.httpFields.put(HttpHeader.CONTENT_LENGTH, multiPartContentProvider.length().toString())
                }
            } else {
                val contentLength = request.contentProvider!!.length()
                if (contentLength >= 0) {
                    request.httpFields.put(HttpHeader.CONTENT_LENGTH, contentLength.toString())
                } else {
                    request.httpFields.put(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED.value)
                }
            }
        }
    }

    if (request.queryParameters.size > 0) {
        if (StringUtils.hasText(request.uri.query)) {
            request.uri.query = request.uri.query + "&" + request.queryParameters.encode(StandardCharsets.UTF_8, true)
        } else {
            request.uri.query = request.queryParameters.encode(StandardCharsets.UTF_8, true)
        }
    }

    if (request.cookies != null) {
        request.httpFields.put(HttpHeader.COOKIE, CookieGenerator.generateCookies(request.cookies))
    }

    val len = request.contentProvider?.length() ?: -1
    val metaDataReq = MetaData.Request(request.method, request.uri, request.httpVersion, request.httpFields, len)
    metaDataReq.trailerSupplier = request.trailerSupplier

    return metaDataReq
}