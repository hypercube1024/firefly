package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.http.common.model.HttpVersion
import java.util.concurrent.CompletableFuture

class AsyncHttpClientConnectionRequestBuilder(
    private val connection: HttpClientConnection,
    method: String,
    uri: HttpURI,
    httpVersion: HttpVersion
) : AbstractHttpClientRequestBuilder(method, uri, httpVersion) {

    override fun submit(): CompletableFuture<HttpClientResponse> = connection.send(httpRequest)

}