package com.fireflysource.net.http.client.impl

import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.pool.AsyncPool
import com.fireflysource.common.pool.PooledObject
import com.fireflysource.common.pool.asyncPool
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.AioTcpClient
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import kotlinx.coroutines.future.await
import java.util.concurrent.ConcurrentHashMap

class TcpConnectionPool(
    secureEngineFactory: SecureEngineFactory? = null,
    val timeout: Long = 30,
    val maxSize: Int = 16,
    val leakDetectorInterval: Long = 60,
    val releaseTimeout: Long = 60
) {

    companion object {
        private val log = SystemLogger.create(TcpConnectionPool::class.java)
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

    private val connPoolMap = ConcurrentHashMap<HostPortScheme, AsyncPool<TcpConnection>>()

    suspend fun getConnection(hostPortScheme: HostPortScheme): PooledObject<TcpConnection> =
        connPoolMap.computeIfAbsent(hostPortScheme) { hps ->
            asyncPool {
                maxSize = this@TcpConnectionPool.maxSize
                timeout = this@TcpConnectionPool.timeout
                leakDetectorInterval = this@TcpConnectionPool.leakDetectorInterval
                releaseTimeout = this@TcpConnectionPool.releaseTimeout

                objectFactory { pool ->
                    asyncGlobally(pool.getCoroutineDispatcher()) {
                        val conn = createConnection(hps)
                        PooledObject(conn, pool) { log.warn("The TCP connection leak. ${conn.id}") }
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

    private suspend fun createConnection(hostPortScheme: HostPortScheme): TcpConnection {
        val conn = if (hostPortScheme.isSecure()) {
            secureTcpClient.connect(hostPortScheme.host, hostPortScheme.port).await()
        } else {
            tcpClient.connect(hostPortScheme.host, hostPortScheme.port).await()
        }
        conn.startReading()
        return conn
    }
}

data class HostPortScheme(val host: String, val port: Int, val scheme: String) {
    fun isSecure(): Boolean {
        return when (scheme) {
            "wss", "https" -> true
            "ws", "http" -> false
            else -> throw IllegalArgumentException("not support the protocol: $scheme")
        }
    }
}

