package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientContentProvider
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.impl.content.handler.StringContentHandler
import com.fireflysource.net.http.client.impl.content.provider.MultiPartContentProvider
import com.fireflysource.net.http.client.impl.content.provider.StringContentProvider
import com.fireflysource.net.http.common.codec.CookieGenerator
import com.fireflysource.net.http.common.codec.UrlEncoded
import com.fireflysource.net.http.common.model.*
import java.nio.charset.StandardCharsets
import java.util.function.Supplier

class AsyncHttpClientRequest : HttpClientRequest {

    companion object {
        private val defaultHttpUri = HttpURI("http://localhost:8080/")
        private val defaultMethod = HttpMethod.GET.value
    }

    private var method: String = defaultMethod
    private var uri: HttpURI = defaultHttpUri
    private var httpVersion: HttpVersion = HttpVersion.HTTP_1_1
    private var queryStrings: UrlEncoded? = null
    private var formInputs: UrlEncoded? = null
    private var httpFields: HttpFields = HttpFields()
    private var cookies: MutableList<Cookie>? = null
    private var trailerSupplier: Supplier<HttpFields>? = null
    private var contentProvider: HttpClientContentProvider? = null
    private var contentHandler: HttpClientContentHandler? = null
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

    override fun getQueryStrings(): UrlEncoded {
        val query = this.queryStrings
        return if (query != null) {
            query
        } else {
            val urlEncoded = UrlEncoded()
            this.queryStrings = urlEncoded
            urlEncoded
        }
    }

    override fun setQueryStrings(queryStrings: UrlEncoded?) {
        this.queryStrings = queryStrings
    }

    override fun getFormInputs(): UrlEncoded {
        val form = this.formInputs
        return if (form != null) {
            form
        } else {
            val urlEncoded = UrlEncoded()
            this.formInputs = urlEncoded
            urlEncoded
        }
    }

    override fun setFormInputs(formInputs: UrlEncoded?) {
        this.formInputs = formInputs
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

    override fun setContentHandler(contentHandler: HttpClientContentHandler?) {
        this.contentHandler = contentHandler
    }

    override fun getContentHandler(): HttpClientContentHandler? = contentHandler

    override fun setHttp2Settings(http2Settings: Map<Int, Int>?) {
        this.http2Settings = http2Settings
    }

    override fun getHttp2Settings(): Map<Int, Int>? = http2Settings

}

fun toMetaDataRequest(request: HttpClientRequest): MetaData.Request {
    if (request.formInputs.size > 0) {
        val formStr = request.formInputs.encode(StandardCharsets.UTF_8, true)
        val stringContentProvider = StringContentProvider(formStr, StandardCharsets.UTF_8)
        request.contentProvider = stringContentProvider
        request.httpFields.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.FORM_ENCODED.value)
        request.httpFields.put(HttpHeader.CONTENT_LENGTH, stringContentProvider.length().toString())
    } else {
        val provider = request.contentProvider
        if (provider != null) {
            if (provider is MultiPartContentProvider) {
                request.httpFields.put(HttpHeader.CONTENT_TYPE, provider.contentType)
                if (provider.length() >= 0) {
                    request.httpFields.put(HttpHeader.CONTENT_LENGTH, provider.length().toString())
                }
            } else {
                val contentLength = provider.length()
                if (contentLength >= 0) {
                    request.httpFields.put(HttpHeader.CONTENT_LENGTH, contentLength.toString())
                } else {
                    request.httpFields.put(HttpHeader.TRANSFER_ENCODING, HttpHeaderValue.CHUNKED.value)
                }
            }
        }
    }

    val uri = if (request.queryStrings.size > 0) {
        val uri = HttpURI(request.uri)
        if (request.uri.hasQuery()) {
            uri.query = request.uri.query + "&" + request.queryStrings.encode(StandardCharsets.UTF_8, true)
        } else {
            uri.query = request.queryStrings.encode(StandardCharsets.UTF_8, true)
        }
        uri
    } else request.uri

    if (request.cookies != null) {
        request.httpFields.put(HttpHeader.COOKIE, CookieGenerator.generateCookies(request.cookies))
    }

    if (request.contentHandler == null) {
        request.contentHandler = StringContentHandler(Long.MAX_VALUE)
    }

    val len = request.contentProvider?.length() ?: -1
    val metaDataReq = MetaData.Request(request.method, uri, request.httpVersion, request.httpFields, len)
    metaDataReq.trailerSupplier = request.trailerSupplier
    return metaDataReq
}