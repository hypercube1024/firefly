package com.fireflysource.net.http.client.impl

import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.pool.AsyncPool
import com.fireflysource.common.pool.PooledObject
import com.fireflysource.common.pool.asyncPool
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientConnectionManager
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.addHttp2UpgradeHeader
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.isUpgradeSuccess
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.removeHttp2UpgradeHeader
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class AsyncHttpClientConnectionManager(
    secureEngineFactory: SecureEngineFactory? = null,
    val timeout: Long = 30,
    val maxSize: Int = 16,
    val leakDetectorInterval: Long = 60,
    val releaseTimeout: Long = 60,
    val requestHeaderBufferSize: Int = 4 * 1024,
    val contentBufferSize: Int = 8 * 1024
) : HttpClientConnectionManager, AbstractLifeCycle() {


    companion object {
        private val log = SystemLogger.create(AsyncHttpClientConnectionManager::class.java)
    }

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
    private val connectionMap = ConcurrentHashMap<Address, AsyncPool<HttpClientConnection>>()


    private suspend fun createConnection(address: Address): TcpConnection {
        val conn = if (address.secure) {
            secureTcpClient.connect(address.socketAddress).await()
        } else {
            tcpClient.connect(address.socketAddress).await()
        }
        conn.startReading()
        return conn
    }

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> = asyncGlobally {
        val socketAddress = InetSocketAddress(request.uri.host, request.uri.port)
        val secure = isSecureProtocol(request.uri.scheme)
        val address = Address(socketAddress, secure)
        val httpResponseFuture = CompletableFuture<HttpClientResponse>()

        val pooledObject = connectionMap.computeIfAbsent(address) { addr ->
            asyncPool {
                maxSize = this@AsyncHttpClientConnectionManager.maxSize
                timeout = this@AsyncHttpClientConnectionManager.timeout
                leakDetectorInterval = this@AsyncHttpClientConnectionManager.leakDetectorInterval
                releaseTimeout = this@AsyncHttpClientConnectionManager.releaseTimeout

                objectFactory { pool ->
                    asyncGlobally(pool.getCoroutineDispatcher()) {
                        val connection = createConnection(addr)
                        val httpConnection: HttpClientConnection = if (connection.isSecureConnection) {
                            when (connection.onHandshakeComplete().await()) {
                                "h2" -> {
                                    Http2ClientConnection(connection)
                                }
                                else -> {
                                    Http1ClientConnection(connection, requestHeaderBufferSize, contentBufferSize)
                                }
                            }
                        } else {
                            // detect the protocol version using the Upgrade header
                            addHttp2UpgradeHeader(request)

                            val http1ClientConnection =
                                Http1ClientConnection(connection, requestHeaderBufferSize, contentBufferSize)
                            val response = http1ClientConnection.send(request).await()

                            if (isUpgradeSuccess(response)) {
                                http1ClientConnection.closeRequestChannel()

                                // switch the protocol to http2
                                val http2ClientConnection = Http2ClientConnection(connection)
                                removeHttp2UpgradeHeader(request)
                                val http2Response = http2ClientConnection.send(request).await()

                                httpResponseFuture.complete(http2Response)
                                http2ClientConnection
                            } else {
                                httpResponseFuture.complete(response)
                                http1ClientConnection
                            }
                        }

                        PooledObject(httpConnection, pool) { log.warn("The TCP connection leak. ${httpConnection.id}") }
                    }.await()
                }

                validator { pooledObject ->
                    !pooledObject.getObject().isClosed
                }

                dispose { pooledObject ->
                    pooledObject.getObject().close()
                }

                noLeakCallback {
                    log.info("no leak TCP connection pool.")
                }
            }

        }.getPooledObject()

        if (httpResponseFuture.isDone) {
            pooledObject.release()
            httpResponseFuture.await()
        } else {
            pooledObject.use { o -> o.getObject().send(request) }.await()
        }
    }.asCompletableFuture()


    override fun init() {
    }

    override fun destroy() {
        tcpClient.stop()
        secureTcpClient.stop()
        connectionMap.values.forEach { it.stop() }
        connectionMap.clear()
    }
}

fun isSecureProtocol(scheme: String): Boolean {
    return when (scheme) {
        "wss", "https" -> true
        "ws", "http" -> false
        else -> throw IllegalArgumentException("not support the protocol: $scheme")
    }
}

data class Address(val socketAddress: InetSocketAddress, val secure: Boolean)