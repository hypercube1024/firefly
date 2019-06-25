package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.client.impl.content.handler.ByteBufferContentHandler
import com.fireflysource.net.http.client.impl.content.handler.StringContentHandler
import com.fireflysource.net.http.common.codec.CookieParser
import com.fireflysource.net.http.common.model.*
import java.nio.ByteBuffer
import java.nio.charset.Charset
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

    override fun getStringBody(): String {
        return if (contentHandler != null && contentHandler is StringContentHandler) {
            contentHandler.toString()
        } else {
            ""
        }
    }

    override fun getStringBody(charset: Charset): String {
        return if (contentHandler != null && contentHandler is StringContentHandler) {
            contentHandler.toString(charset)
        } else {
            ""
        }
    }

    override fun getBody(): MutableList<ByteBuffer> {
        return if (contentHandler != null && contentHandler is ByteBufferContentHandler) {
            contentHandler.byteBufferList
        } else {
            mutableListOf()
        }
    }
}