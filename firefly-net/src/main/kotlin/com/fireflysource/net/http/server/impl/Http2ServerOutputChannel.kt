package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.common.v2.stream.Stream
import com.fireflysource.net.http.server.HttpServerOutputChannel
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture

/**
 * @author Pengtao Qiu
 */
class Http2ServerOutputChannel(
    private val http2ServerConnection: Http2ServerConnection,
    private val response: MetaData.Response,
    private val stream: Stream
) : HttpServerOutputChannel {

    override fun commit(): CompletableFuture<Void> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isCommitted(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun write(byteBuffers: Array<ByteBuffer>, offset: Int, length: Int): CompletableFuture<Long> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun write(byteBufferList: MutableList<ByteBuffer>, offset: Int, length: Int): CompletableFuture<Long> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun write(string: String): CompletableFuture<Int> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun write(string: String, charset: Charset): CompletableFuture<Int> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun write(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isOpen(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun closeFuture(): CompletableFuture<Void> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}