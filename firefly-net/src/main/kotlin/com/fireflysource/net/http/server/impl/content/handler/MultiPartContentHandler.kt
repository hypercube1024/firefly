package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.net.http.server.HttpServerContentHandler
import com.fireflysource.net.http.server.MultiPart
import com.fireflysource.net.http.server.RoutingContext
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

class MultiPartContentHandler(
    private val maxFileSize: Long = 0,
    private val maxRequestSize: Long = 0,
    private val fileSizeThreshold: Int = 0
) : HttpServerContentHandler {

    private val multiParts: List<MultiPart> = mutableListOf()
    private var firstMessage = true

    fun getPart(name: String): MultiPart? {
        return multiParts.find { it.name == name }
    }

    fun getParts(): List<MultiPart> = multiParts

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

sealed class MultiPartHandlerMessage
class ParseMultiPartBoundary(val contentType: String) : MultiPartHandlerMessage()
class ParseMultiPartContent(val byteBuffer: ByteBuffer) : MultiPartHandlerMessage()
object EndMultiPartHandler : MultiPartHandlerMessage()