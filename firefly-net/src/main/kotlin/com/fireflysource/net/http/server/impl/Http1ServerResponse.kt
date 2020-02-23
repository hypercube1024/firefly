package com.fireflysource.net.http.server.impl

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.server.HttpServerOutputChannel
import java.util.concurrent.CompletableFuture

class Http1ServerResponse(private val http1ServerConnection: Http1ServerConnection) :
    AbstractHttpServerResponse(http1ServerConnection) {

    companion object {
        private val response100Buffer = BufferUtils.toBuffer("HTTP/1.1 100 Continue\r\n")
    }

    override fun createHttpServerOutputChannel(response: MetaData.Response): HttpServerOutputChannel {
        return Http1ServerOutputChannel(http1ServerConnection, response)
    }

    override fun response100Continue(): CompletableFuture<Void> {
        return http1ServerConnection.tcpConnection.write(response100Buffer.duplicate()).thenCompose { Result.DONE }
    }
}