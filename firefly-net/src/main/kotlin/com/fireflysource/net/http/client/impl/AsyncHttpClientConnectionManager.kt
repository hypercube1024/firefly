package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientConnectionManager
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import java.util.concurrent.CompletableFuture

class AsyncHttpClientConnectionManager(
    timeout: Long,
    secureEngineFactory: SecureEngineFactory?
) : HttpClientConnectionManager {

    private val tcpClient: TcpClient = AioTcpClient().timeout(timeout)
    private val secureTcpClient: TcpClient = if (secureEngineFactory != null) {
        AioTcpClient()
            .timeout(timeout)
            .secureEngineFactory(secureEngineFactory)
            .enableSecureConnection()
    } else {
        AioTcpClient()
            .timeout(timeout)
            .enableSecureConnection()
    }

    override fun getConnection(request: HttpClientRequest): CompletableFuture<HttpClientConnection> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}