package com.fireflysource.net.http.client.impl

import com.fireflysource.common.func.Callback
import com.fireflysource.common.io.OutputChannel
import com.fireflysource.net.http.client.*
import com.fireflysource.net.http.client.impl.content.provider.ByteBufferProvider
import com.fireflysource.net.http.client.impl.content.provider.MultiPartContentProvider
import com.fireflysource.net.http.client.impl.content.provider.StringBodyProvider
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.Cookie
import com.fireflysource.net.http.common.model.HttpField
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpHeader
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Supplier

class AsyncHttpClientRequestBuilder(
    private val connectionManager: HttpClientConnectionManager
) : HttpClientRequestBuilder {

    private val httpRequest: AsyncHttpClientRequest = AsyncHttpClientRequest()
    private val multiPartContentProvider: MultiPartContentProvider by lazy { MultiPartContentProvider() }

    override fun cookies(cookies: MutableList<Cookie>?): HttpClientRequestBuilder {
        httpRequest.cookies = cookies
        return this
    }

    override fun put(name: String, list: MutableList<String>): HttpClientRequestBuilder {
        httpRequest.httpFields.put(name, list)
        return this
    }

    override fun put(header: HttpHeader, value: String): HttpClientRequestBuilder {
        httpRequest.httpFields.put(header, value)
        return this
    }

    override fun put(name: String, value: String): HttpClientRequestBuilder {
        httpRequest.httpFields.put(name, value)
        return this
    }

    override fun put(field: HttpField): HttpClientRequestBuilder {
        httpRequest.httpFields.put(field)
        return this
    }

    override fun addAll(fields: HttpFields): HttpClientRequestBuilder {
        httpRequest.httpFields.addAll(fields)
        return this
    }

    override fun add(field: HttpField): HttpClientRequestBuilder {
        httpRequest.httpFields.add(field)
        return this
    }

    override fun trailerSupplier(trailerSupplier: Supplier<HttpFields>?): HttpClientRequestBuilder {
        httpRequest.trailerSupplier = trailerSupplier
        return this
    }

    override fun body(content: String): HttpClientRequestBuilder = body(content, StandardCharsets.UTF_8)

    override fun body(content: String, charset: Charset): HttpClientRequestBuilder =
        contentProvider(StringBodyProvider(content, charset))

    override fun body(buffer: ByteBuffer): HttpClientRequestBuilder = contentProvider(ByteBufferProvider(buffer))

    override fun output(outputChannel: Consumer<OutputChannel>): HttpClientRequestBuilder {
        httpRequest.outputChannel = outputChannel
        return this
    }

    override fun contentProvider(contentProvider: HttpClientContentProvider?): HttpClientRequestBuilder {
        httpRequest.contentProvider = contentProvider
        return this
    }

    override fun addFieldPart(
        name: String,
        content: HttpClientContentProvider,
        fields: HttpFields?
    ): HttpClientRequestBuilder {
        contentProvider(multiPartContentProvider)
        multiPartContentProvider.addFieldPart(name, content, fields)
        return this
    }

    override fun addFilePart(
        name: String,
        fileName: String,
        content: HttpClientContentProvider,
        fields: HttpFields?
    ): HttpClientRequestBuilder {
        contentProvider(multiPartContentProvider)
        multiPartContentProvider.addFilePart(name, fileName, content, fields)
        return this
    }

    override fun addFormParam(name: String, value: String): HttpClientRequestBuilder {
        httpRequest.formParameters.add(name, value)
        return this
    }

    override fun addFormParam(name: String, values: MutableList<String>): HttpClientRequestBuilder {
        httpRequest.formParameters.addValues(name, values)
        return this
    }

    override fun putFormParam(name: String, value: String): HttpClientRequestBuilder {
        httpRequest.formParameters.put(name, value)
        return this
    }

    override fun putFormParam(name: String, values: MutableList<String>): HttpClientRequestBuilder {
        httpRequest.formParameters.putValues(name, values)
        return this
    }

    override fun removeFormParam(name: String): HttpClientRequestBuilder {
        httpRequest.formParameters.remove(name)
        return this
    }

    override fun addQueryParam(name: String, value: String): HttpClientRequestBuilder {
        httpRequest.queryParameters.add(name, value)
        return this
    }

    override fun addQueryParam(name: String, values: MutableList<String>): HttpClientRequestBuilder {
        httpRequest.queryParameters.addValues(name, values)
        return this
    }

    override fun putQueryParam(name: String, value: String): HttpClientRequestBuilder {
        httpRequest.queryParameters.put(name, value)
        return this
    }

    override fun putQueryParam(name: String, values: MutableList<String>): HttpClientRequestBuilder {
        httpRequest.queryParameters[name] = values
        return this
    }

    override fun removeQueryParam(name: String): HttpClientRequestBuilder {
        httpRequest.queryParameters.remove(name)
        return this
    }

    override fun headerComplete(headerComplete: Consumer<HttpClientResponse>?): HttpClientRequestBuilder {
        httpRequest.headerComplete = headerComplete
        return this
    }

    override fun contentHandler(contentHandler: HttpClientContentHandler?): HttpClientRequestBuilder {
        httpRequest.contentHandler = contentHandler
        return this
    }

    override fun contentComplete(contentComplete: Consumer<HttpClientResponse>?): HttpClientRequestBuilder {
        httpRequest.contentComplete = contentComplete
        return this
    }

    override fun messageComplete(messageComplete: Consumer<HttpClientResponse>?): HttpClientRequestBuilder {
        httpRequest.messageComplete = messageComplete
        return this
    }

    override fun badMessage(badMessage: Consumer<BadMessageException>?): HttpClientRequestBuilder {
        httpRequest.badMessage = badMessage
        return this
    }

    override fun earlyEof(earlyEof: Callback?): HttpClientRequestBuilder {
        httpRequest.earlyEof = earlyEof
        return this
    }

    override fun http2Settings(http2Settings: Map<Int, Int>?): HttpClientRequestBuilder {
        httpRequest.http2Settings = http2Settings
        return this
    }

    override fun submit(): CompletableFuture<HttpClientResponse> {
        return connectionManager.send(httpRequest)
    }
}