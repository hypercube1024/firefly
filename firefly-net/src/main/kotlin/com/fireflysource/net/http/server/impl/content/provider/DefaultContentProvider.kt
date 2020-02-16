package com.fireflysource.net.http.server.impl.content.provider

import com.fireflysource.net.http.server.HttpServerContentProvider
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

class DefaultContentProvider(val status: Int, val exception: Throwable?) : HttpServerContentProvider {

    override fun length(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isOpen(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun toByteBuffer(): ByteBuffer {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun closeFuture(): CompletableFuture<Void> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun read(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}