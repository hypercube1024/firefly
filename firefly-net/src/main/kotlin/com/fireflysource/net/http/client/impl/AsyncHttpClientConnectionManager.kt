package com.fireflysource.net.http.client.impl

import com.fireflysource.common.concurrent.CompletableFutures
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
import com.fireflysource.net.http.common.exception.MissingRemoteHostException
import com.fireflysource.net.http.common.exception.MissingRemotePortException
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.http.common.model.isCloseConnection
import com.fireflysource.net.tcp.TcpClientConnectionFactory
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.ApplicationProtocol
import com.fireflysource.net.tcp.aio.isSecureProtocol
import com.fireflysource.net.tcp.aio.schemaDefaultPort
import kotlinx.coroutines.future.await
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class AsyncHttpClientConnectionManager(
    private val config: HttpConfig,
    private val connectionFactory: TcpClientConnectionFactory
) : HttpClientConnectionManager, AbstractLifeCycle() {

    companion object {
        private val log = SystemLogger.create(AsyncHttpClientConnectionManager::class.java)
    }

    private val connectionPoolMap = ConcurrentHashMap<Address, AsyncPool<HttpClientConnection>>()

    init {
        start()
    }

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        val address = buildAddress(request.uri)
        val nonPersistence = request.httpFields.isCloseConnection(request.httpVersion)
        return if (nonPersistence) sendByNonPersistenceConnection(address, request) else sendByPool(address, request)
    }

    override fun createHttpClientConnection(httpURI: HttpURI): CompletableFuture<HttpClientConnection> {
        val address = buildAddress(httpURI)
        return createHttpClientConnection(address)
    }

    private fun sendByPool(address: Address, request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        val pool = connectionPoolMap.computeIfAbsent(address) { buildHttpClientConnectionPool(it) }
        val retryCount = config.clientRetryCount
        return if (retryCount > 0) {
            CompletableFutures.retry(
                retryCount,
                { sendByPool(request, pool) },
                { e, count -> log.warn { "retry to send http request. message: ${e.message}, retry count: $count" } }
            )
        } else sendByPool(request, pool)
    }

    private fun sendByPool(request: HttpClientRequest, pool: AsyncPool<HttpClientConnection>) =
        pool.poll().thenCompose { pooledObject ->
            val connection = pooledObject.getObject()
            pooledObject.use { connection.send(request) }
//            val connection = pooledObject.getObject()
//            if (connection.httpVersion == HttpVersion.HTTP_2) {
//                pooledObject.use { connection.send(request) }
//            } else {
//                connection.send(request).doFinally { _, _ -> pooledObject.closeFuture() }
//            }
        }

    private fun sendByNonPersistenceConnection(address: Address, request: HttpClientRequest) =
        connectionFactory.connect(
            address.socketAddress,
            address.secure,
            listOf(ApplicationProtocol.HTTP1.value)
        ).thenCompose { connection ->
            connection.beginHandshake().thenApply { createHttp1ClientConnection(connection) }
        }.thenCompose { sendAndCloseConnection(it, request) }

    private fun sendAndCloseConnection(connection: HttpClientConnection, request: HttpClientRequest) =
        connection.send(request).thenCompose { response -> connection.closeFuture().thenApply { response } }


    private fun buildAddress(uri: HttpURI): Address {
        if (uri.host.isNullOrBlank()) {
            throw MissingRemoteHostException("The host is missing. uri: $uri")
        }
        val port: Int = if (uri.port > 0) {
            uri.port
        } else {
            schemaDefaultPort[uri.scheme] ?: throw MissingRemotePortException("The address port is missing. uri: $uri")
        }
        val socketAddress = InetSocketAddress(uri.host, port)
        val secure = isSecureProtocol(uri.scheme)
        return Address(socketAddress, secure)
    }

    private fun buildHttpClientConnectionPool(address: Address): AsyncPool<HttpClientConnection> = asyncPool {
        maxSize = config.connectionPoolSize
        timeout = config.timeout
        leakDetectorInterval = config.leakDetectorInterval
        releaseTimeout = config.releaseTimeout

        objectFactory { pool ->
            val httpConnection = createHttpClientConnection(address).await()
            PooledObject(httpConnection, pool) { log.warn("The TCP connection leak. ${httpConnection.id}") }
        }

        validator { pooledObject ->
            !pooledObject.getObject().isInvalid
        }

        dispose { pooledObject ->
            pooledObject.getObject().close()
        }

        noLeakCallback {
            log.info("no leak TCP connection pool.")
        }
    }

    private fun createHttpClientConnection(address: Address): CompletableFuture<HttpClientConnection> {
        return createTcpConnection(address).thenCompose { connection ->
            val httpConnection = if (connection.isSecureConnection) {
                connection.beginHandshake().thenApply { applicationProtocol ->
                    when (applicationProtocol) {
                        ApplicationProtocol.HTTP2.value -> createHttp2ClientConnection(connection)
                        else -> createHttp1ClientConnection(connection)
                    }
                }
            } else CompletableFuture.completedFuture(createHttp1ClientConnection(connection))
            httpConnection
        }
    }

    private fun createTcpConnection(address: Address): CompletableFuture<TcpConnection> =
        connectionFactory.connect(address.socketAddress, address.secure)

    private fun createHttp1ClientConnection(connection: TcpConnection): HttpClientConnection {
        return Http1ClientConnection(config, connection)
    }

    private fun createHttp2ClientConnection(connection: TcpConnection): HttpClientConnection {
        return Http2ClientConnection(config, connection)
    }

    override fun init() {
        connectionFactory.start()
    }

    override fun destroy() {
        connectionPoolMap.values.forEach { it.stop() }
        connectionPoolMap.clear()
        connectionFactory.stop()
    }

    private data class Address(val socketAddress: InetSocketAddress, val secure: Boolean)
}