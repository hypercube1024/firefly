package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.HttpField
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.v1.decoder.HttpParser
import java.nio.ByteBuffer

class Http1ClientResponseHandler : HttpParser.ResponseHandler {

    override fun getHeaderCacheSize(): Int {
        return 4096
    }

    override fun startResponse(version: HttpVersion, status: Int, reason: String): Boolean {
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

    override fun parsedTrailer(field: HttpField?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun messageComplete(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun badMessage(failure: BadMessageException) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun earlyEOF() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}