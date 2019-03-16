package com.fireflysource.net.http.client.impl

import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.pool.Pool
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientConnectionManager
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.lang.IllegalStateException
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

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> = asyncGlobally {
        val reqHostPort = ReqHostPort(request.uri.host, request.uri.port, request.uri.scheme)
        val httpVersion: HttpVersion? = reqHostPortHttpVersionMap[reqHostPort]
        if (httpVersion != null) {
            when (httpVersion) {
                HttpVersion.HTTP_2 -> {

                }
                HttpVersion.HTTP_1_1 -> {

                }
                else -> throw IllegalStateException("not support the protocol: $httpVersion")
            }
        } else {
            // unknown the protocol version
        }

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }.asCompletableFuture()

    private suspend fun createConnection(request: HttpClientRequest): TcpConnection {
        return when (request.uri.scheme) {
            "http" -> {
                tcpClient.connect(request.uri.host, request.uri.port).await()
            }
            "https" -> {
                secureTcpClient.connect(request.uri.host, request.uri.port).await()
            }
            else -> throw IllegalArgumentException("not support the protocol: ${request.uri.scheme}")
        }
    }
}

data class ReqHostPort(val host: String, val port: Int, val scheme: String)

data class RequestEvent(val request: HttpClientRequest, val responseFuture: CompletableFuture<HttpClientResponse>)