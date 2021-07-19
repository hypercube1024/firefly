package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientRequestBuilder
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.http.common.model.HttpVersion

interface AbstractHttpClientConnection : HttpClientConnection {

    override fun request(method: String, httpURI: HttpURI): HttpClientRequestBuilder {
        return AsyncHttpClientConnectionRequestBuilder(this, method, httpURI, HttpVersion.HTTP_1_1)
    }

}