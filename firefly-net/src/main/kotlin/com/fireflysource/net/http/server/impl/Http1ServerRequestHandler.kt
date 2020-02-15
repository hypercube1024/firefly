package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.model.HttpField
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import com.fireflysource.net.http.server.HttpServerRequest
import java.nio.ByteBuffer

class Http1ServerRequestHandler : HttpParser.RequestHandler {

    override fun startRequest(method: String, uri: String, version: HttpVersion): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getHeaderCacheSize(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun parsedHeader(field: HttpField) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun headerComplete(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun content(item: ByteBuffer): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun contentComplete(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun messageComplete(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun earlyEOF() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend fun toHttpServerRequest(): HttpServerRequest {
        TODO("not implemented")
    }
}