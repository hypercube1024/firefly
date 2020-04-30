package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.client.impl.content.handler.ByteBufferContentHandler
import com.fireflysource.net.http.client.impl.content.handler.StringContentHandler
import com.fireflysource.net.http.common.codec.CookieParser
import com.fireflysource.net.http.common.model.*
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Supplier

class AsyncHttpClientResponse(
    val response: MetaData.Response,
    private val contentHandler: HttpClientContentHandler?
) : HttpClientResponse {

    private val cookieList: List<Cookie> by lazy {
        httpFields.getValuesList(HttpHeader.SET_COOKIE).map {
            CookieParser.parseSetCookie(it)
        }
    }

    override fun getStatus(): Int = response.status

    override fun getReason(): String =
        Optional.ofNullable(response.reason).orElseGet { HttpStatus.getMessage(response.status) }

    override fun getHttpVersion(): HttpVersion = response.httpVersion

    override fun getHttpFields(): HttpFields = response.fields

    override fun getCookies(): List<Cookie> = cookieList

    override fun getContentLength(): Long = response.contentLength

    override fun getTrailerSupplier(): Supplier<HttpFields> =
        Optional.ofNullable(response.trailerSupplier).orElseGet { Supplier { HttpFields() } }

    override fun getStringBody(): String = getStringBody(StandardCharsets.UTF_8)

    override fun getStringBody(charset: Charset): String = Optional
        .ofNullable(contentHandler)
        .filter { it is StringContentHandler }
        .map { it as StringContentHandler }
        .map { it.toString(charset, getContentEncoding()) }
        .orElse("")

    override fun getBody(): List<ByteBuffer> = Optional
        .ofNullable(contentHandler)
        .filter { it is ByteBufferContentHandler }
        .map { it as ByteBufferContentHandler }
        .map { it.getByteBuffers(getContentEncoding()) }
        .orElse(listOf())

    private fun getContentEncoding(): Optional<ContentEncoding> {
        return Optional.ofNullable(this.httpFields[HttpHeader.CONTENT_ENCODING])
            .map { it.trim() }
            .map { it.toLowerCase() }
            .flatMap { ContentEncoding.from(it) }
    }

    override fun toString(): String {
        return """
            |response: -----------------
            |$status $reason $httpVersion
            |$httpFields
            |$stringBody
            |${trailerSupplier.get()}
            |end response --------------
        """.trimMargin()
    }
}