package com.fireflysource.net.http.client.impl

import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.net.http.client.HttpClientConnectionManager
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.tcp.aio.Address
import com.fireflysource.net.tcp.aio.AioTcpClientConnectionPool
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture

class AsyncHttpClientConnectionManager(
    private val connectionPool: AioTcpClientConnectionPool
) : HttpClientConnectionManager {

//    private val connMap = mutableMapOf<Address, HttpClientConnection>()

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> = asyncGlobally {
        val secure = isSecureProtocol(request.uri.scheme)
        val address = Address(InetSocketAddress(request.uri.host, request.uri.port), secure)
        val pooledObject = connectionPool.getConnection(address)
        val connection = pooledObject.getObject()
        if (connection.isSecureConnection) {
            val protocol = if (connection.isHandshakeComplete) {
                connection.applicationProtocol
            } else {
                connection.onHandshakeComplete().await()
            }

            when (protocol) {
                "h2" -> {

                }
                else -> {

                }
            }
        } else {

        }

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }.asCompletableFuture()


    private fun isSecureProtocol(scheme: String): Boolean {
        return when (scheme) {
            "wss", "https" -> true
            "ws", "http" -> false
            else -> throw IllegalArgumentException("not support the protocol: $scheme")
        }
    }


//    private val mutex = Mutex()
//    private val connPoolMap: MutableMap<ReqHostPort, HttpConnectionPool> = ConcurrentHashMap()

//    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> = asyncGlobally {
//        mutex.withLock {
//            val reqHostPort = ReqHostPort(request.uri.host, request.uri.port, request.uri.scheme)
//            val httpConnPool: HttpConnectionPool? = connPoolMap[reqHostPort]
//            if (httpConnPool != null) {
//                val httpClientConnection = httpConnPool.getHttpClientConnection()
//                val response = httpClientConnection.use { it.getObject().send(request).await() }
//                response
//            } else {
//                // unknown the protocol version
//                if (reqHostPort.isSecure()) {
//                    // detect the protocol version using ALPN
//                    val secureTcpConnection = createConnection(reqHostPort)
//                    when (secureTcpConnection.onHandshakeComplete().await()) {
//                        "h2" -> {
//                            val http2Conn = Http2ClientConnection(secureTcpConnection)
//                            val http2ConnPool = Http2ConnectionPool(reqHostPort, http2Conn)
//                            connPoolMap[reqHostPort] = http2ConnPool
//                            val response = http2Conn.send(request).await()
//                            response
//                        }
//                        else -> {
//                            val http1ClientConnection = Http1ClientConnection(secureTcpConnection)
//                            val response = http1ClientConnection.use { it.send(request).await() }
//                            val http1ConnPool = createHttp1ConnectionPool(reqHostPort)
//                            connPoolMap[reqHostPort] = http1ConnPool
//                            response
//                        }
//                    }
//                } else {
//                    addHttp2UpgradeHeader(request)
//
//                    val tcpConn = createConnection(reqHostPort)
//                    val http1ClientConnection = Http1ClientConnection(tcpConn)
//                    val response = http1ClientConnection.use { http1ClientConnection.send(request).await() }
//
//                    if (isUpgradeSuccess(response)) {
//                        // switch the protocol to http2
//                        val http2Conn = Http2ClientConnection(tcpConn)
//                        val http2ConnPool = Http2ConnectionPool(reqHostPort, http2Conn)
//                        connPoolMap[reqHostPort] = http2ConnPool
//
//                        removeHttp2UpgradeHeader(request)
//
//                        val http2Resp = http2Conn.send(request).await()
//                        http2Resp
//                    } else {
//                        val http1ConnPool = createHttp1ConnectionPool(reqHostPort)
//                        connPoolMap[reqHostPort] = http1ConnPool
//                        response
//                    }
//                }
//            }
//        }
//    }.asCompletableFuture()


//    private suspend fun createHttp1ConnectionPool(reqHostPort: ReqHostPort): Http1ConnectionPool {
//
//    }
//
//    class Http1ConnectionPool(
//        private val reqHostPort: ReqHostPort,
//        private val connPool: Pool<HttpClientConnection>
//    ) : HttpConnectionPool {
//
//        override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_1_1
//
//        override suspend fun getHttpClientConnection(): PooledObject<HttpClientConnection> {
//            return connPool.poll().await()
//        }
//    }

//    private inner class Http2ConnectionPool(
//        private val reqHostPort: ReqHostPort,
//        private var conn: Http2ClientConnection
//    ) : HttpConnectionPool {
//
//        override fun getHttpVersion(): HttpVersion = HttpVersion.HTTP_2
//
//        override suspend fun getHttpClientConnection(): PooledObject<HttpClientConnection> {
//            return if (conn.isClosed) {
//                val tcpConn = createConnection(reqHostPort)
//                conn = Http2ClientConnection(tcpConn)
//                FakePooledObject(conn)
//            } else {
//                FakePooledObject(conn)
//            }
//        }
//    }

}

//interface HttpConnectionPool {
//
//    fun getHttpVersion(): HttpVersion
//
//    suspend fun getHttpClientConnection(): PooledObject<HttpClientConnection>
//
//}