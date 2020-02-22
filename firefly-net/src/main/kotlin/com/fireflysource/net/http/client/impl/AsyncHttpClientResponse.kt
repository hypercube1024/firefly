package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.client.impl.content.handler.ByteBufferContentHandler
import com.fireflysource.net.http.client.impl.content.handler.StringContentHandler
import com.fireflysource.net.http.common.codec.CookieParser
import com.fireflysource.net.http.common.model.*
import java.nio.ByteBuffer
import java.nio.charset.Charset
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

    override fun getReason(): String = response.reason

    override fun getHttpVersion(): HttpVersion = response.httpVersion

    override fun getHttpFields(): HttpFields = response.fields

    override fun getCookies(): List<Cookie> = cookieList

    override fun getContentLength(): Long = response.contentLength

    override fun getTrailerSupplier(): Supplier<HttpFields> = response.trailerSupplier

    override fun getStringBody(): String = Optional
        .ofNullable(contentHandler)
        .filter { it is StringContentHandler }
        .map { it.toString() }
        .orElse("")

    override fun getStringBody(charset: Charset): String = Optional
        .ofNullable(contentHandler)
        .filter { it is StringContentHandler }
        .map { it as StringContentHandler }
        .map { it.toString(charset) }
        .orElse("")

    override fun getBody(): List<ByteBuffer> = Optional
        .ofNullable(contentHandler)
        .filter { it is ByteBufferContentHandler }
        .map { it as ByteBufferContentHandler }
        .map { it.getByteBuffers() }
        .orElse(listOf())

    override fun toString(): String {
        return """
            |response: -----------------
            |$status
            |$httpFields
            |$stringBody
            |end response --------------
        """.trimMargin()
    }
}