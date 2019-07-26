package com.fireflysource.net.http.client.impl

import com.fireflysource.common.coroutine.CoroutineDispatchers.singleThread
import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.pool.AsyncPool
import com.fireflysource.common.pool.PooledObject
import com.fireflysource.common.pool.asyncPool
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.*
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.addHttp2UpgradeHeader
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.isUpgradeSuccess
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.removeHttp2UpgradeHeader
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.aio.SupportedProtocolEnum
import com.fireflysource.net.tcp.aio.isSecureProtocol
import com.fireflysource.net.tcp.aio.schemaDefaultPort
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture

class AsyncHttpClientConnectionManager(
    private val config: HttpClientConfig = HttpClientConfig()
) : HttpClientConnectionManager, AbstractLifeCycle() {

    companion object {
        private val log = SystemLogger.create(AsyncHttpClientConnectionManager::class.java)
    }

    private val tcpClient: TcpClient = AioTcpClient().timeout(config.timeout)
    private val secureTcpClient: TcpClient = if (config.secureEngineFactory != null) {
        AioTcpClient()
            .timeout(config.timeout)
            .secureEngineFactory(config.secureEngineFactory)
            .enableSecureConnection()
    } else {
        AioTcpClient()
            .timeout(config.timeout)
            .enableSecureConnection()
    }
    private val connectionMap = HashMap<Address, AsyncPool<HttpClientConnection>>()

    init {
        start()
    }

    private suspend fun createConnection(address: Address): TcpConnection {
        val conn = if (address.secure) {
            secureTcpClient.connect(address.socketAddress).await()
        } else {
            tcpClient.connect(address.socketAddress).await()
        }
        conn.startReading()
        return conn
    }

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> = asyncGlobally(singleThread) {
        val port: Int = if (request.uri.port > 0) {
            request.uri.port
        } else {
            schemaDefaultPort[request.uri.scheme] ?: throw IllegalArgumentException("The port is missing")
        }
        val socketAddress = InetSocketAddress(request.uri.host, port)
        val secure = isSecureProtocol(request.uri.scheme)
        val address = Address(socketAddress, secure)

        val pooledHttpClientConnection = connectionMap.computeIfAbsent(address) { addr ->
            asyncPool {
                maxSize = config.connectionPoolSize
                timeout = config.timeout
                leakDetectorInterval = config.leakDetectorInterval
                releaseTimeout = config.releaseTimeout

                objectFactory { pool ->
                    val connection = createConnection(addr)
                    val httpConnection: HttpClientConnection = if (connection.isSecureConnection) {
                        when (connection.onHandshakeComplete().await()) {
                            SupportedProtocolEnum.H2.value -> {
                                Http2ClientConnection(connection, config.maxDynamicTableSize, config.maxHeaderSize)
                            }
                            else -> {
                                Http1ClientConnection(
                                    connection,
                                    config.requestHeaderBufferSize,
                                    config.contentBufferSize
                                )
                            }
                        }
                    } else {
                        Http1ClientConnection(connection, config.requestHeaderBufferSize, config.contentBufferSize)
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

        }.getPooledObject()

        pooledHttpClientConnection.use { o ->
            val httpClientConnection = o.getObject()
            if (!httpClientConnection.isSecureConnection
                && httpClientConnection.httpVersion == HttpVersion.HTTP_1_1
                && httpClientConnection is Http1ClientConnection
            ) {
                val attachment = httpClientConnection.attachment

                if (attachment != null && attachment is HttpClientConnection) {
                    attachment.send(request)
                } else {
                    // detect the protocol version using the Upgrade header
                    addHttp2UpgradeHeader(request)

                    val response = httpClientConnection.send(request).await()
                    if (isUpgradeSuccess(response)) {
                        httpClientConnection.cancelRequestJob()

                        // switch the protocol to http2
                        val http2ClientConnection = Http2ClientConnection(
                            httpClientConnection.tcpConnection,
                            config.maxDynamicTableSize,
                            config.maxHeaderSize
                        )
                        httpClientConnection.attachment = http2ClientConnection
                        log.info { "HTTP1 connection ${httpClientConnection.id} upgrade HTTP2 success" }

                        removeHttp2UpgradeHeader(request)
                        http2ClientConnection.send(request)
                    } else {
                        httpClientConnection.attachment = httpClientConnection
                        log.info { "HTTP1 connection ${httpClientConnection.id} upgrade HTTP2 failure" }

                        val httpResponseFuture = CompletableFuture<HttpClientResponse>()
                        httpResponseFuture.complete(response)
                        httpResponseFuture
                    }
                }
            } else {
                httpClientConnection.send(request)
            }
        }.await()
    }.asCompletableFuture()


    override fun init() {
    }

    override fun destroy() {
        connectionMap.values.forEach { it.stop() }
        connectionMap.clear()
        tcpClient.stop()
        secureTcpClient.stop()
    }
}

data class Address(val socketAddress: InetSocketAddress, val secure: Boolean)