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
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.aio.SupportedProtocolEnum
import com.fireflysource.net.tcp.aio.isSecureProtocol
import com.fireflysource.net.tcp.aio.schemaDefaultPort
import kotlinx.coroutines.future.await
import java.net.InetSocketAddress
import java.util.concurrent.CancellationException
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
            AioTcpClient().timeout(config.timeout)
                .secureEngineFactory(config.secureEngineFactory).enableSecureConnection()
        } else {
            AioTcpClient().timeout(config.timeout)
                .enableSecureConnection()
        }
    }

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        val address = buildAddress(request)
        return connectionPoolMap
            .computeIfAbsent(address) { buildHttpClientConnectionPool(it) }
            .poll()
            .thenCompose { it.send(request, address) }
    }

    private fun PooledObject<HttpClientConnection>.send(
        request: HttpClientRequest,
        address: Address
    ): CompletableFuture<HttpClientResponse> {
        val connection = this.getObject()
        log.debug { "get client connection. id: ${connection.id}, closed: ${connection.isClosed}, version: ${connection.httpVersion}" }
        return this.use {
            connection.sendRequest(request).exceptionallyCompose { e ->
                retryOneTimeWhenCancellationException(connection, e, request, address)
            }
        }
    }

    private fun retryOneTimeWhenCancellationException(
        connection: HttpClientConnection,
        e: Throwable,
        request: HttpClientRequest,
        address: Address
    ): CompletableFuture<HttpClientResponse> {
        return if (connection.httpVersion == HttpVersion.HTTP_1_1 && e.cause != null && e.cause is CancellationException) {
            log.warn("retry request: ${request.uri}, message: ${e.message}, class: ${e.cause?.javaClass?.name}")
            retryOneTime(address, request)
        } else CompletableFutures.completeExceptionally(e)
    }

    private fun retryOneTime(address: Address, request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        return createTcpConnection(address)
            .thenApply { createHttp1ClientConnection(it) }
            .thenCompose { connection -> retrySend(connection, request) }
    }

    private fun retrySend(connection: HttpClientConnection, request: HttpClientRequest) =
        connection.send(request).thenCompose { response -> connection.closeFuture().thenApply { response } }

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
            val connection = createTcpConnection(address).await()
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

    private fun createTcpConnection(address: Address): CompletableFuture<TcpConnection> {
        return if (address.secure) {
            secureTcpClient.connect(address.socketAddress)
        } else {
            tcpClient.connect(address.socketAddress)
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