package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.codec.UrlEncoded
import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.RoutingContext
import kotlinx.coroutines.future.await
import java.net.URL
import java.nio.ByteBuffer

class Http1ServerRequestHandler(val connection: HttpServerConnection) : HttpParser.RequestHandler {

    var connectionListener: HttpServerConnection.Listener? = null
    private var request = MetaData.Request(HttpFields())
    private var context: AsyncRoutingContext? = null

    override fun startRequest(method: String, uri: String, version: HttpVersion): Boolean {
        request.method = method
        request.uri = HttpURI(URL(uri).toURI())
        request.httpVersion = version
        return false
    }

    override fun getHeaderCacheSize(): Int = 4096

    override fun parsedHeader(field: HttpField) {
        request.fields.add(field)
    }

    override fun headerComplete(): Boolean {
        context = newRoutingContext()
        connectionListener?.onHeaderComplete(context)
        return false
    }

    private fun newRoutingContext(): AsyncRoutingContext {
        val httpServerRequest = AsyncHttpServerRequest(MetaData.Request(request))
        val query: String? = request.uri.query
        if (query != null && query.isNotBlank()) {
            val urlEncoded = UrlEncoded()
            urlEncoded.decode(query)
            httpServerRequest.urlEncoded = urlEncoded
        }
        return AsyncRoutingContext(httpServerRequest, AsyncHttpServerResponse(connection), connection)
    }

    override fun content(byteBuffer: ByteBuffer): Boolean {
        context?.request?.contentHandler?.accept(byteBuffer, context)
        return false
    }

    override fun contentComplete(): Boolean {
        return false
    }

    override fun messageComplete(): Boolean {
        return true
    }

    override fun earlyEOF() {
    }

    suspend fun complete(): RoutingContext {
        context?.request?.contentHandler?.closeFuture()?.await()
        return context!!
    }

    fun getAsyncRoutingContext() = context

    fun reset() {
        request.recycle()
        context = null
    }

}