package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.common.v1.decoder.containExpectContinue
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.HttpServerRequest
import com.fireflysource.net.http.server.HttpServerResponse
import com.fireflysource.net.http.server.RoutingContext
import com.fireflysource.net.http.server.impl.content.provider.DefaultContentProvider
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class AsyncRoutingContext(
    private val request: HttpServerRequest,
    private val response: HttpServerResponse,
    private val connection: HttpServerConnection
) : RoutingContext {

    private val attributes: ConcurrentHashMap<String, Any> by lazy { ConcurrentHashMap<String, Any>() }

    override fun getAttribute(key: String): Any? = attributes[key]

    override fun setAttribute(key: String, value: Any): Any? = attributes.put(key, value)

    override fun getAttributes(): MutableMap<String, Any> = attributes

    override fun removeAttribute(key: String): Any? = attributes.remove(key)

    override fun getRequest(): HttpServerRequest = request

    override fun getResponse(): HttpServerResponse = response

    override fun getPathParameter(name: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPathParameter(index: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPathParameterByRegexGroup(index: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun expect100Continue(): Boolean {
        return request.httpFields.containExpectContinue()
    }

    override fun redirect(url: String): CompletableFuture<Void> {
        val status = HttpStatus.FOUND_302
        return setStatus(status)
            .put(HttpHeader.LOCATION, url)
            .contentProvider(DefaultContentProvider(status, null, this))
            .end()
    }

    override fun <T> next(): CompletableFuture<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasNext(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getConnection(): HttpServerConnection = connection

}