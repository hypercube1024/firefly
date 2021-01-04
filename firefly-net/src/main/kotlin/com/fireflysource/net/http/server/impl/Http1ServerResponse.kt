package com.fireflysource.net.http.server.impl

import com.fireflysource.common.io.toBuffer
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.server.HttpServerOutputChannel
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class Http1ServerResponse(
    private val http1ServerConnection: Http1ServerConnection,
    private val expect100Continue: Boolean,
    private val closeConnection: Boolean
) : AbstractHttpServerResponse(http1ServerConnection) {

    private val write100Continue = AtomicBoolean(false)
    private val write200ConnectionEstablished = AtomicBoolean(false)

    override fun createHttpServerOutputChannel(response: MetaData.Response): HttpServerOutputChannel {
        if (expect100Continue && !write100Continue.get()) {
            http1ServerConnection.resetParser()
        }
        return Http1ServerOutputChannel(http1ServerConnection, response, closeConnection)
    }

    override fun response100Continue(): CompletableFuture<Void> {
        return if (write100Continue.compareAndSet(false, true)) {
            writeAndFlushHttpMessage("HTTP/1.1 100 Continue\r\n".toBuffer())
        } else Result.DONE
    }

    override fun response200ConnectionEstablished(): CompletableFuture<Void> {
        return if (write200ConnectionEstablished.compareAndSet(false, true)) {
            writeAndFlushHttpMessage("HTTP/1.1 200 Connection Established\r\n\r\n".toBuffer())
        } else Result.DONE
    }

    private fun writeAndFlushHttpMessage(message: ByteBuffer): CompletableFuture<Void> {
        val connection = http1ServerConnection.tcpConnection
        return connection.writeAndFlush(message).thenCompose { Result.DONE }
    }

}