package com.fireflysource.net.http.client.impl

import com.fireflysource.common.concurrent.CompletableFutures
import com.fireflysource.common.concurrent.exceptionallyCompose
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.pool.AsyncPool
import com.fireflysource.common.pool.PooledObject
import com.fireflysource.common.pool.asyncPool
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientConnectionManager
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.aio.SupportedProtocolEnum
import com.fireflysource.net.tcp.aio.isSecureProtocol
import com.fireflysource.net.tcp.aio.schemaDefaultPort
import kotlinx.coroutines.future.await
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class AsyncHttpClientConnectionManager(
    private val config: HttpConfig = HttpConfig()
) : HttpClientConnectionManager, AbstractLifeCycle() {

    companion object {
        private val log = SystemLogger.create(AsyncHttpClientConnectionManager::class.java)
    }

    private val tcpClient: TcpClient = AioTcpClient().timeout(config.timeout)
    private val secureTcpClient: TcpClient = createSecureTcpClient()
    private val connectionPoolMap = ConcurrentHashMap<Address, AsyncPool<HttpClientConnection>>()

    init {
        start()
    }

    private fun createSecureTcpClient(): TcpClient {
        return if (config.secureEngineFactory != null) {
            AioTcpClient()
                .timeout(config.timeout)
                .secureEngineFactory(config.secureEngineFactory)
                .enableSecureConnection()
        } else {
            AioTcpClient()
                .timeout(config.timeout)
                .enableSecureConnection()
        }
    }

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        return sendRetry(request, 0, config.clientMaxRetry)
//        val address = buildAddress(request)
//        val pool = connectionPoolMap.computeIfAbsent(address) { buildHttpClientConnectionPool(it) }
//        return pool.poll()
//            .thenCompose { it.send(request) }
//            .exceptionallyCompose {
//                val future = send(request)
//                println("retry request: ${request.uri}")
//                future
//            }
//        return connectionPoolMap
//            .computeIfAbsent(address) { buildHttpClientConnectionPool(it) }
//            .poll()
//            .thenCompose { it.send(request) }
    }

    private fun sendRetry(
        request: HttpClientRequest,
        retryCount: Int,
        maxRetry: Int
    ): CompletableFuture<HttpClientResponse> {
        val address = buildAddress(request)
        val pool = connectionPoolMap.computeIfAbsent(address) { buildHttpClientConnectionPool(it) }
        return pool.poll()
            .thenCompose { it.send(request) }
            .exceptionallyCompose {
                if (retryCount < maxRetry) {
                    val future = sendRetry(request, retryCount + 1, maxRetry)
                    log.warn("retry request: ${request.uri}, count: $retryCount, max: $maxRetry, message: ${it.message}")
                    future
                } else {
                    CompletableFutures.completeExceptionally(it)
                }
            }
    }

    private fun PooledObject<HttpClientConnection>.send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        val connection = this.getObject()
        log.debug { "get client connection. id: ${connection.id}, closed: ${connection.isClosed}, version: ${connection.httpVersion}" }
        return this.use { connection.sendRequest(request) }
    }

    private fun HttpClientConnection.sendRequest(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        return when {
            this.isSecureConnection -> this.send(request)
            this is Http1ClientConnection -> this.sendRequestTryToUpgradeHttp2(request)
            else -> this.send(request)
        }
    }

    private fun buildAddress(request: HttpClientRequest): Address {
        val port: Int = if (request.uri.port > 0) {
            request.uri.port
        } else {
            schemaDefaultPort[request.uri.scheme] ?: throw IllegalArgumentException("The port is missing")
        }
        val socketAddress = InetSocketAddress(request.uri.host, port)
        val secure = isSecureProtocol(request.uri.scheme)
        return Address(socketAddress, secure)
    }

    private fun buildHttpClientConnectionPool(address: Address): AsyncPool<HttpClientConnection> = asyncPool {
        maxSize = config.connectionPoolSize
        timeout = config.timeout
        leakDetectorInterval = config.leakDetectorInterval
        releaseTimeout = config.releaseTimeout

        objectFactory { pool ->
            val connection = createTcpConnection(address)
            val httpConnection = if (connection.isSecureConnection) {
                when (connection.beginHandshake().await()) {
                    SupportedProtocolEnum.H2.value -> createHttp2ClientConnection(connection)
                    else -> createHttp1ClientConnection(connection)
                }
            } else {
                createHttp1ClientConnection(connection)
            }

            PooledObject(httpConnection, pool) { log.warn("The TCP connection leak. ${httpConnection.id}") }
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

    private suspend fun createTcpConnection(address: Address): TcpConnection {
        return if (address.secure) {
            secureTcpClient.connect(address.socketAddress).await()
        } else {
            tcpClient.connect(address.socketAddress).await()
        }
    }

    private fun createHttp1ClientConnection(connection: TcpConnection): HttpClientConnection {
        return Http1ClientConnection(config, connection)
    }

    private fun createHttp2ClientConnection(connection: TcpConnection): HttpClientConnection {
        return Http2ClientConnection(config, connection)
    }

    override fun init() {
    }

    override fun destroy() {
        connectionPoolMap.values.forEach { it.stop() }
        connectionPoolMap.clear()
        tcpClient.stop()
        secureTcpClient.stop()
    }

    private data class Address(val socketAddress: InetSocketAddress, val secure: Boolean)
}