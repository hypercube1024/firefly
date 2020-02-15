package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.codec.CookieParser
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.server.HttpServerContentHandler
import com.fireflysource.net.http.server.HttpServerRequest
import com.fireflysource.net.http.server.MultiPart
import com.fireflysource.net.http.server.impl.content.handler.ByteBufferContentHandler
import com.fireflysource.net.http.server.impl.content.handler.StringContentHandler
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
import java.util.function.Supplier

class AsyncHttpServerRequest(
    val request: MetaData.Request,
    private var contentHandler: HttpServerContentHandler = StringContentHandler()
) : HttpServerRequest {

    private var cookieList: List<Cookie>? = null

    override fun getMethod(): String = request.method

    override fun getURI(): HttpURI = request.uri

    override fun getHttpVersion(): HttpVersion = request.httpVersion

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

    override fun getContentHandler(): HttpServerContentHandler? = this.contentHandler

    override fun setContentHandler(contentHandler: HttpServerContentHandler) {
        this.contentHandler = contentHandler
    }

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

    override fun getPart(name: String?): MultiPart {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getParts(): MutableList<MultiPart> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTrailerSupplier(): Supplier<HttpFields> = request.trailerSupplier

    fun reset() {
        request.recycle()
        cookieList = null
        contentHandler = StringContentHandler()
    }
}