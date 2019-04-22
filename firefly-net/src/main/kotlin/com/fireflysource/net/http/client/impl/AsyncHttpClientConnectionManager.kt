package com.fireflysource.net.http.client.impl

import com.fireflysource.common.codec.base64.Base64Utils
import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.pool.FakePooledObject
import com.fireflysource.common.pool.Pool
import com.fireflysource.common.pool.PooledObject
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientConnectionManager
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.v2.encoder.HeaderGenerator
import com.fireflysource.net.http.common.v2.encoder.SettingsGenerator
import com.fireflysource.net.http.common.v2.frame.SettingsFrame
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
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
                    val protocol = secureTcpConnection.onHandshakeComplete().await()
                    when (protocol) {
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
                    // detect the protocol version using Connection and Upgrade HTTP headers
                    val oldValues: List<String>? = request.httpFields.getValuesList(HttpHeader.CONNECTION)
                    if (!oldValues.isNullOrEmpty()) {
                        val newValues = mutableListOf<String>()
                        newValues.addAll(oldValues)
                        newValues.add("Upgrade")
                        newValues.add("HTTP2-Settings")
                        request.httpFields.addCSV(HttpHeader.CONNECTION, *newValues.toTypedArray())
                    } else {
                        request.httpFields.addCSV(HttpHeader.CONNECTION, "Upgrade", "HTTP2-Settings")
                    }
                    request.httpFields.put(HttpHeader.UPGRADE, "h2c")

                    // generate http2 settings base64
                    val settingsGenerator = SettingsGenerator(HeaderGenerator())
                    val frameBytes = if (request.http2Settings.isNullOrEmpty()) {
                        settingsGenerator.generateSettings(SettingsFrame.DEFAULT_SETTINGS_FRAME.settings, false)
                    } else {
                        settingsGenerator.generateSettings(request.http2Settings, false)
                    }
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    byteArrayOutputStream.use { stream ->
                        frameBytes.byteBuffers.forEach { buffer ->
                            stream.write(BufferUtils.toArray(buffer))
                        }
                    }
                    val bytes = byteArrayOutputStream.toByteArray()
                    val base64 = Base64Utils.encodeToString(bytes)
                    request.httpFields.put(HttpHeader.HTTP2_SETTINGS, base64)


                    val tcpConn = createConnection(reqHostPort)
                    val http1ClientConnection = Http1ClientConnection(tcpConn)
                    val response = http1ClientConnection.use { http1ClientConnection.send(request).await() }

                    val connValue: String? = request.httpFields[HttpHeader.CONNECTION]
                    val upgradeValue: String? = request.httpFields[HttpHeader.UPGRADE]
                    if (response.status == HttpStatus.SWITCHING_PROTOCOLS_101
                        && connValue != null && connValue == "Upgrade"
                        && upgradeValue != null && upgradeValue == "h2c"
                    ) {
                        // switch the protocol to http2
                        val http2Conn = Http2ClientConnection(tcpConn)
                        val http2ConnPool = Http2ConnectionPool(reqHostPort, http2Conn)
                        connPoolMap[reqHostPort] = http2ConnPool

                        request.httpFields.remove(HttpHeader.HTTP2_SETTINGS)
                        request.httpFields.remove(HttpHeader.UPGRADE)
                        request.httpFields.put(HttpHeader.CONNECTION, "keep-alive")
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
        return when (reqHostPort.scheme) {
            "http" -> {
                tcpClient.connect(reqHostPort.host, reqHostPort.port).await()
            }
            "https" -> {
                secureTcpClient.connect(reqHostPort.host, reqHostPort.port).await()
            }
            else -> throw IllegalArgumentException("not support the protocol: ${reqHostPort.scheme}")
        }
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
            return connPool.get().await()
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