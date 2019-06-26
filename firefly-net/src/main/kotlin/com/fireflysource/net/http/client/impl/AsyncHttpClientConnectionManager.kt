package com.fireflysource.net.http.client.impl

import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.pool.FakePooledObject
import com.fireflysource.common.pool.Pool
import com.fireflysource.common.pool.PooledObject
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientConnectionManager
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.addHttp2UpgradeHeader
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.isUpgradeSuccess
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.removeHttp2UpgradeHeader
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val mutex = Mutex()
    private val connPoolMap: MutableMap<ReqHostPort, HttpConnectionPool> = ConcurrentHashMap()

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> = asyncGlobally {
        mutex.withLock {
            val reqHostPort = ReqHostPort(request.uri.host, request.uri.port, request.uri.scheme)
            val httpConnPool: HttpConnectionPool? = connPoolMap[reqHostPort]
            if (httpConnPool != null) {
                val httpClientConnection = httpConnPool.getHttpClientConnection()
                val response = httpClientConnection.use { it.`object`.send(request).await() }
                response
            } else {
                // unknown the protocol version
                if (reqHostPort.isSecure()) {
                    // detect the protocol version using ALPN
                    val secureTcpConnection = createConnection(reqHostPort)
                    when (secureTcpConnection.onHandshakeComplete().await()) {
                        "h2" -> {
                            val http2Conn = Http2ClientConnection(secureTcpConnection)
                            val http2ConnPool = Http2ConnectionPool(reqHostPort, http2Conn)
                            connPoolMap[reqHostPort] = http2ConnPool
                            val response = http2Conn.send(request).await()
                            response
                        }
                        else -> {
                            val http1ClientConnection = Http1ClientConnection(secureTcpConnection)
                            val response = http1ClientConnection.use { it.send(request).await() }
                            val http1ConnPool = createHttp1ConnectionPool(reqHostPort)
                            connPoolMap[reqHostPort] = http1ConnPool
                            response
                        }
                    }
                } else {
                    addHttp2UpgradeHeader(request)

                    val tcpConn = createConnection(reqHostPort)
                    val http1ClientConnection = Http1ClientConnection(tcpConn)
                    val response = http1ClientConnection.use { http1ClientConnection.send(request).await() }

                    if (isUpgradeSuccess(response)) {
                        // switch the protocol to http2
                        val http2Conn = Http2ClientConnection(tcpConn)
                        val http2ConnPool = Http2ConnectionPool(reqHostPort, http2Conn)
                        connPoolMap[reqHostPort] = http2ConnPool

                        removeHttp2UpgradeHeader(request)

                        val http2Resp = http2Conn.send(request).await()
                        http2Resp
                    } else {
                        val http1ConnPool = createHttp1ConnectionPool(reqHostPort)
                        connPoolMap[reqHostPort] = http1ConnPool
                        response
                    }
                }
            }
        }
    }.asCompletableFuture()

    private suspend fun createConnection(reqHostPort: ReqHostPort): TcpConnection {
        val conn = when (reqHostPort.scheme) {
            "http" -> {
                tcpClient.connect(reqHostPort.host, reqHostPort.port).await()
            }
            "https" -> {
                secureTcpClient.connect(reqHostPort.host, reqHostPort.port).await()
            }
            else -> throw IllegalArgumentException("not support the protocol: ${reqHostPort.scheme}")
        }
        conn.startReading()
        return conn
    }

    private suspend fun createHttp1ConnectionPool(reqHostPort: ReqHostPort): Http1ConnectionPool {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    class Http1ConnectionPool(
        private val reqHostPort: ReqHostPort,
        private val connPool: Pool<HttpClientConnection>
    ) : HttpConnectionPool {

        override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_1_1

        override suspend fun getHttpClientConnection(): PooledObject<HttpClientConnection> {
            return connPool.poll().await()
        }
    }

    private inner class Http2ConnectionPool(
        private val reqHostPort: ReqHostPort,
        private var conn: Http2ClientConnection
    ) : HttpConnectionPool {

        override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_2

        override suspend fun getHttpClientConnection(): PooledObject<HttpClientConnection> {
            return if (conn.isClosed) {
                val tcpConn = createConnection(reqHostPort)
                conn = Http2ClientConnection(tcpConn)
                FakePooledObject(conn)
            } else {
                FakePooledObject(conn)
            }
        }
    }

}

data class ReqHostPort(val host: String, val port: Int, val scheme: String) {
    fun isSecure(): Boolean {
        return when (scheme) {
            "https" -> true
            "http" -> false
            else -> throw IllegalArgumentException("not support the protocol: $scheme")
        }
    }
}

interface HttpConnectionPool {

    fun getHttpVersion(): HttpVersion

    suspend fun getHttpClientConnection(): PooledObject<HttpClientConnection>

}