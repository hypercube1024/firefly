package com.fireflysource.net.http.client.impl

import com.fireflysource.common.coroutine.launchSingle
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
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.aio.SupportedProtocolEnum
import com.fireflysource.net.tcp.aio.isSecureProtocol
import com.fireflysource.net.tcp.aio.schemaDefaultPort
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.future.await
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture

class AsyncHttpClientConnectionManager(
    private val config: HttpConfig = HttpConfig()
) : HttpClientConnectionManager, AbstractLifeCycle() {

    companion object {
        private val log = SystemLogger.create(AsyncHttpClientConnectionManager::class.java)
    }

    private val tcpClient: TcpClient = AioTcpClient().timeout(config.timeout)
    private val secureTcpClient: TcpClient = createSecureTcpClient()
    private val connectionPoolMap = HashMap<Address, AsyncPool<HttpClientConnection>>()
    private val requestChannel = Channel<RequestMessage>(Channel.UNLIMITED)
    private val job = requestJob()

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
        val responseFuture = CompletableFuture<HttpClientResponse>()
        val requestMessage = RequestMessage(request, responseFuture)
        requestChannel.offer(requestMessage)
        return responseFuture
    }

    private fun requestJob() = launchSingle {
        while (true) {
            val request = requestChannel.receive()
            handleRequestMessage(request)
        }
    }

    private suspend fun handleRequestMessage(requestMessage: RequestMessage) {
        val (request, responseFuture) = requestMessage
        val port: Int = if (request.uri.port > 0) {
            request.uri.port
        } else {
            schemaDefaultPort[request.uri.scheme] ?: throw IllegalArgumentException("The port is missing")
        }
        val socketAddress = InetSocketAddress(request.uri.host, port)
        val secure = isSecureProtocol(request.uri.scheme)
        val address = Address(socketAddress, secure)

        val pooledObject = connectionPoolMap
            .computeIfAbsent(address) { buildHttpClientConnectionPool(it) }
            .takePooledObject()

        fun sendRequest() {
            pooledObject.use { it.getObject().send(request) }
                .thenAccept { responseFuture.complete(it) }
                .exceptionally {
                    responseFuture.completeExceptionally(it)
                    null
                }
        }

        if (pooledObject.getObject().isSecureConnection) {
            sendRequest()
        } else {
            val http1ClientConnection = pooledObject.getObject() as Http1ClientConnection
            val upgradeHttpClientConnection = http1ClientConnection.upgradeHttpClientConnection

            if (upgradeHttpClientConnection != null) {
                log.debug { "Upgrade to HTTP2 connection and send request. " }
                pooledObject.use { upgradeHttpClientConnection.send(request) }
                    .thenAccept { responseFuture.complete(it) }
                    .exceptionally {
                        responseFuture.completeExceptionally(it)
                        null
                    }
            } else {
                if (http1ClientConnection.sendHttp2UpgradeHeaders) {
                    try {
                        val response =
                            pooledObject.use { sendRequestWithHttp2UpgradeHeader(request, http1ClientConnection) }
                        responseFuture.complete(response)
                    } catch (e: Exception) {
                        responseFuture.completeExceptionally(e)
                    }
                } else {
                    log.debug { "Not support HTTP2 and use HTTP1 send request. id: ${http1ClientConnection.id}" }
                    sendRequest()
                }
            }
        }
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

    private suspend fun sendRequestWithHttp2UpgradeHeader(
        request: HttpClientRequest,
        http1ClientConnection: Http1ClientConnection
    ): HttpClientResponse {
        log.debug { "Try to add h2c headers. id: ${http1ClientConnection.id}" }
        // detect the protocol version using the Upgrade header
        addHttp2UpgradeHeader(request)

        val response = http1ClientConnection.send(request).await()
        http1ClientConnection.sendHttp2UpgradeHeaders = false

        return if (isUpgradeSuccess(response)) {
            // switch the protocol to HTTP2
            val http2ClientConnection = createHttp2ClientConnection(http1ClientConnection.tcpConnection)
            http1ClientConnection.upgradeHttpClientConnection = http2ClientConnection

            log.info { "HTTP1 connection upgrades HTTP2 success. id: ${http1ClientConnection.id}" }

            removeHttp2UpgradeHeader(request)
            http2ClientConnection.send(request).await()
        } else {
            log.info { "HTTP1 connection upgrades HTTP2 failure. id: ${http1ClientConnection.id}" }
            response
        }
    }

    override fun init() {
    }

    override fun destroy() {
        connectionPoolMap.values.forEach { it.stop() }
        connectionPoolMap.clear()
        tcpClient.stop()
        secureTcpClient.stop()
        job.cancel()
    }

    private data class Address(val socketAddress: InetSocketAddress, val secure: Boolean)
    private data class RequestMessage(
        val request: HttpClientRequest,
        val response: CompletableFuture<HttpClientResponse>
    )
}