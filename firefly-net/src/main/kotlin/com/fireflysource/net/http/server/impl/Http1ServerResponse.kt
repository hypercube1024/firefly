package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.server.HttpServerOutputChannel

class Http1ServerResponse(private val http1ServerConnection: Http1ServerConnection) :
    AbstractHttpServerResponse(http1ServerConnection) {

    override fun createHttpServerOutputChannel(response: MetaData.Response): HttpServerOutputChannel {
        return Http1ServerOutputChannel(http1ServerConnection, response)
    }
}