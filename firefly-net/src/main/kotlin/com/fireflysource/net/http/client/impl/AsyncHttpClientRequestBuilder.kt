package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientConnectionManager
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.http.common.model.HttpVersion
import java.util.concurrent.CompletableFuture

class AsyncHttpClientRequestBuilder(
    private val connectionManager: HttpClientConnectionManager,
    method: String,
    uri: HttpURI,
    httpVersion: HttpVersion
) : AbstractHttpClientRequestBuilder(method, uri, httpVersion) {

    override fun submit(): CompletableFuture<HttpClientResponse> = connectionManager.send(httpRequest)

}