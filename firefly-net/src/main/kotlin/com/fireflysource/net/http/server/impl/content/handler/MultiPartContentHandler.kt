package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.net.http.server.HttpServerContentHandler
import com.fireflysource.net.http.server.MultiPart
import com.fireflysource.net.http.server.RoutingContext
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

class MultiPartContentHandler : HttpServerContentHandler {

    fun getPart(name: String): MultiPart {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getParts(): List<MultiPart> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun accept(byteBuffer: ByteBuffer, context: RoutingContext) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun closeFuture(): CompletableFuture<Void> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}