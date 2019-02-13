package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientConnectionManager
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import java.util.concurrent.CompletableFuture

class AsyncHttpClientConnectionManager(
    private val timeout: Long,
    private val secureEngineFactory: SecureEngineFactory?
) : HttpClientConnectionManager {


    override fun getConnection(request: HttpClientRequest?): CompletableFuture<HttpClientConnection> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}