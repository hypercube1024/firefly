package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.codec.CookieParser
import com.fireflysource.net.http.common.codec.UrlEncoded
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.server.HttpServerContentHandler
import com.fireflysource.net.http.server.HttpServerRequest
import com.fireflysource.net.http.server.MultiPart
import com.fireflysource.net.http.server.impl.content.handler.ByteBufferContentHandler
import com.fireflysource.net.http.server.impl.content.handler.FormInputsContentHandler
import com.fireflysource.net.http.server.impl.content.handler.MultiPartContentHandler
import com.fireflysource.net.http.server.impl.content.handler.StringContentHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

class AsyncHttpServerRequest(
    val request: MetaData.Request,
    config: HttpConfig,
    scope: CoroutineScope = CoroutineScope(CoroutineName("Firefly-HTTP-server-request"))
) : HttpServerRequest {

    private var cookieList: List<Cookie>? = null
    private var queryStringMap: UrlEncoded? = null
    private val requestComplete = AtomicBoolean(false)
    private var contentHandler: HttpServerContentHandler

    init {
        val query: String? = request.uri.query
        if (query != null && query.isNotBlank()) {
            queryStringMap = UrlEncoded(query)
        }
        val contentType = request.fields[HttpHeader.CONTENT_TYPE]
        contentHandler = if (contentType != null) {
            when {
                contentType.contains("x-www-form-urlencoded", true) ->
                    FormInputsContentHandler(config.maxRequestBodySize)
                contentType.contains("multipart/form-data", true) ->
                    MultiPartContentHandler(
                        config.maxUploadFileSize,
                        config.maxRequestBodySize,
                        config.uploadFileSizeThreshold,
                        scope
                    )
                else -> StringContentHandler(config.maxRequestBodySize)
            }
        } else StringContentHandler(config.maxRequestBodySize)

    }

    override fun getMethod(): String = request.method

    override fun getURI(): HttpURI = request.uri

    override fun getHttpVersion(): HttpVersion = request.httpVersion

    override fun getQueryString(name: String): String = queryStringMap?.getString(name) ?: ""

    override fun getQueryStrings(name: String): List<String> = queryStringMap?.get(name) ?: listOf()

    override fun getQueryStrings(): Map<String, List<String>> = queryStringMap ?: mapOf()

    override fun getHttpFields(): HttpFields = request.fields

    override fun getContentLength(): Long = request.contentLength

    override fun getCookies(): List<Cookie> {
        val cookies = cookieList
        return if (cookies == null) {
            val list = Optional.ofNullable(httpFields[HttpHeader.COOKIE])
                .filter { it.isNotBlank() }
                .map { CookieParser.parseCookie(it) }
                .orElse(listOf())
            cookieList = list
            list
        } else {
            cookies
        }
    }

    override fun getContentHandler(): HttpServerContentHandler = this.contentHandler

    override fun setContentHandler(contentHandler: HttpServerContentHandler) {
        this.contentHandler = contentHandler
    }

    override fun isRequestComplete(): Boolean = requestComplete.get()

    override fun setRequestComplete(requestComplete: Boolean) {
        this.requestComplete.set(requestComplete)
    }

    override fun getStringBody(): String = getStringBody(StandardCharsets.UTF_8)

    override fun getStringBody(charset: Charset): String = Optional
        .ofNullable(contentHandler)
        .filter { isRequestComplete }
        .filter { it is StringContentHandler }
        .map { it as StringContentHandler }
        .map { it.toString(charset, getContentEncoding()) }
        .orElse("")

    override fun getBody(): List<ByteBuffer> = Optional
        .ofNullable(contentHandler)
        .filter { isRequestComplete }
        .filter { it is ByteBufferContentHandler }
        .map { it as ByteBufferContentHandler }
        .map { it.getByteBuffers(getContentEncoding()) }
        .orElse(listOf())

    override fun getFormInput(name: String): String = Optional
        .ofNullable(contentHandler)
        .filter { isRequestComplete }
        .filter { it is FormInputsContentHandler }
        .map { it as FormInputsContentHandler }
        .map { it.getFormInput(name, getContentEncoding()) }
        .orElse("")

    override fun getFormInputs(name: String): List<String> = Optional
        .ofNullable(contentHandler)
        .filter { isRequestComplete }
        .filter { it is FormInputsContentHandler }
        .map { it as FormInputsContentHandler }
        .map { it.getFormInputs(name, getContentEncoding()) }
        .orElse(listOf())

    override fun getFormInputs(): Map<String, List<String>> = Optional
        .ofNullable(contentHandler)
        .filter { isRequestComplete }
        .filter { it is FormInputsContentHandler }
        .map { it as FormInputsContentHandler }
        .map { it.getFormInputs(getContentEncoding()) }
        .orElse(mapOf())

    override fun getPart(name: String): MultiPart? = Optional
        .ofNullable(contentHandler)
        .filter { isRequestComplete }
        .filter { it is MultiPartContentHandler }
        .map { it as MultiPartContentHandler }
        .map { it.getPart(name) }
        .orElse(null)

    override fun getParts(): List<MultiPart> = Optional
        .ofNullable(contentHandler)
        .filter { isRequestComplete }
        .filter { it is MultiPartContentHandler }
        .map { it as MultiPartContentHandler }
        .map { it.getParts() }
        .orElse(listOf())

    override fun getTrailerSupplier(): Supplier<HttpFields> = request.trailerSupplier

    private fun getContentEncoding(): Optional<ContentEncoding> {
        return Optional.ofNullable(this.httpFields[HttpHeader.CONTENT_ENCODING])
            .map { it.trim() }
            .map { it.lowercase(Locale.getDefault()) }
            .flatMap { ContentEncoding.from(it) }
    }

    override fun toString(): String {
        return """
            |request: -----------------
            |$method $uri $httpVersion
            |$httpFields
            |$stringBody
            |end request --------------
        """.trimMargin()
    }
}