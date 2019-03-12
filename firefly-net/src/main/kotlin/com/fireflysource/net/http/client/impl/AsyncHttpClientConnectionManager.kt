package com.fireflysource.net.http.client.impl

import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.pool.Pool
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientConnectionManager
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

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

    private val reqHostPortHttpVersionMap: Map<ReqHostPort, HttpVersion> = ConcurrentHashMap()
    private val unknowVersionReqChannel = Channel<RequestEvent>(Channel.UNLIMITED)
    private val reqHostPortHttp1ConnPoolMap: Map<ReqHostPort, Pool<HttpClientConnection>> = ConcurrentHashMap()
    private val reqHostHttp2ConnMap: Map<ReqHostPort, HttpClientConnection> = ConcurrentHashMap()


    override fun getConnection(request: HttpClientRequest): CompletableFuture<HttpClientConnection> = asyncGlobally {
        when (request.uri.scheme) {
            "http" -> {
                // TODO
                val tcpConnection = tcpClient.connect(request.uri.host, request.uri.port).await()
                Http1ClientConnection(tcpConnection)
            }
            "https" -> {
                // TODO
                val tcpConnection = secureTcpClient.connect(request.uri.host, request.uri.port).await()
                Http1ClientConnection(tcpConnection)
            }
            else -> throw IllegalArgumentException("not support the protocol: ${request.uri.scheme}")
        }
    }.asCompletableFuture()
}

data class ReqHostPort(val host: String, val port: Int)

data class RequestEvent(val request: HttpClientRequest, val responseFuture: CompletableFuture<HttpClientResponse>)