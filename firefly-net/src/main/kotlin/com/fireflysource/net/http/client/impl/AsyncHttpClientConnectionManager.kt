package com.fireflysource.net.http.client.impl

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

    private suspend fun createConnection(address: Address): TcpConnection {
        val tcpConnection = if (address.secure) {
            secureTcpClient.connect(address.socketAddress).await()
        } else {
            tcpClient.connect(address.socketAddress).await()
        }
        tcpConnection.startReading()
        return tcpConnection
    }

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        val address = buildAddress(request)
        return connectionPoolMap
            .computeIfAbsent(address) { buildHttpClientConnectionPool(it) }
            .poll()
            .thenCompose { pooledObject -> pooledObject.use { send(request, it.getObject()) } }
    }

    private fun send(
        request: HttpClientRequest,
        httpConnection: HttpClientConnection
    ): CompletableFuture<HttpClientResponse> {
        return when {
            httpConnection.isSecureConnection -> httpConnection.send(request)
            httpConnection is Http1ClientConnection -> httpConnection.sendRequestTryToUpgradeHttp2(request)
            else -> httpConnection.send(request)
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
            val connection = createConnection(address)
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