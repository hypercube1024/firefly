package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.model.*
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.RoutingContext
import kotlinx.coroutines.future.await
import java.net.URL
import java.nio.ByteBuffer

class Http1ServerRequestHandler(val connection: HttpServerConnection) : HttpParser.RequestHandler {

    var connectionListener: HttpServerConnection.Listener? = null
    var request = MetaData.Request(HttpFields())
    var httpServerRequest = AsyncHttpServerRequest(request)
    var context = AsyncRoutingContext(httpServerRequest, AsyncHttpServerResponse(), connection)

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
        connectionListener?.onHeaderComplete(context)
        return false
    }

    override fun content(byteBuffer: ByteBuffer): Boolean {
        httpServerRequest.contentHandler?.accept(byteBuffer, context)
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
        httpServerRequest.contentHandler?.closeFuture()?.await()
        return context
    }

    fun reset() {
        connectionListener = null
        context.reset()
    }

}