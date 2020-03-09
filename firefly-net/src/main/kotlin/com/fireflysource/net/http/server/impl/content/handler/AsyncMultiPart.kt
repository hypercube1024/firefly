package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.server.MultiPart
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

class AsyncMultiPart : MultiPart {

    override fun getSubmittedFileName(): String {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun getHttpFields(): HttpFields {
        TODO("Not yet implemented")
    }

    override fun closeFuture(): CompletableFuture<Void> {
        TODO("Not yet implemented")
    }

    override fun getSize(): Long {
        TODO("Not yet implemented")
    }

    override fun getContentType(): String {
        TODO("Not yet implemented")
    }

    override fun read(byteBuffer: ByteBuffer?): CompletableFuture<Int> {
        TODO("Not yet implemented")
    }

    override fun isOpen(): Boolean {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}