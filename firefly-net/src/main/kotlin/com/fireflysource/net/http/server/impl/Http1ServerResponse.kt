package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.model.MetaData
import com.fireflysource.net.http.server.HttpServerConnection
import com.fireflysource.net.http.server.HttpServerOutputChannel

class Http1ServerResponse(httpServerConnection: HttpServerConnection) :
    AbstractHttpServerResponse(httpServerConnection) {

    override fun createHttpServerOutputChannel(response: MetaData.Response): HttpServerOutputChannel {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}