package com.fireflysource.net.http.client.impl

import com.fireflysource.common.`object`.Assert
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientRequestBuilder
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.http.common.model.HttpVersion

interface AbstractHttpClientConnection : HttpClientConnection {

    override fun request(method: String, httpURI: HttpURI): HttpClientRequestBuilder {
        Assert.hasText(httpURI.path, "The http path must be not null.")
        return AsyncHttpClientConnectionRequestBuilder(this, method, httpURI, HttpVersion.HTTP_1_1)
    }

}