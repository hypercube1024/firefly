package com.fireflysource.net.http.server.impl

import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.common.v2.frame.HeadersFrame
import com.fireflysource.net.http.common.v2.stream.Stream
import com.fireflysource.net.http.server.HttpServerOutputChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Pengtao Qiu
 */
class Http2ServerResponse(
    http2ServerConnection: Http2ServerConnection,
    private val stream: Stream
) : AbstractHttpServerResponse(http2ServerConnection) {

    private val write100Continue = AtomicBoolean(false)

    override fun createHttpServerOutputChannel(response: MetaData.Response): HttpServerOutputChannel {
        return Http2ServerOutputChannel(response, stream)
    }

    override fun response100Continue(): CompletableFuture<Void> {
        return if (write100Continue.compareAndSet(false, true)) {
            val response = MetaData.Response(HttpVersion.HTTP_2, HttpStatus.CONTINUE_100, HttpFields())
            val headers = HeadersFrame(stream.id, response, null, false)
            val future = CompletableFuture<Void>()
            stream.headers(headers, Result.futureToConsumer(future))
            future
        } else Result.DONE
    }

    override fun response200ConnectionEstablished(): CompletableFuture<Void> = Result.DONE
}