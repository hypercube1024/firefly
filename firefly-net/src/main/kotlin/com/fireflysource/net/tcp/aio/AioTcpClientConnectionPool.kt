package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.pool.AsyncPool
import com.fireflysource.common.pool.PooledObject
import com.fireflysource.common.pool.asyncPool
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import kotlinx.coroutines.future.await
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

class AioTcpClientConnectionPool(
    secureEngineFactory: SecureEngineFactory? = null,
    val timeout: Long = 30,
    val maxSize: Int = 16,
    val leakDetectorInterval: Long = 60,
    val releaseTimeout: Long = 60
) {

    companion object {
        private val log = SystemLogger.create(AioTcpClientConnectionPool::class.java)
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

    private val connPoolMap = ConcurrentHashMap<Address, AsyncPool<TcpConnection>>()

    suspend fun getConnection(address: Address): PooledObject<TcpConnection> =
        connPoolMap.computeIfAbsent(address) { hps ->
            asyncPool {
                maxSize = this@AioTcpClientConnectionPool.maxSize
                timeout = this@AioTcpClientConnectionPool.timeout
                leakDetectorInterval = this@AioTcpClientConnectionPool.leakDetectorInterval
                releaseTimeout = this@AioTcpClientConnectionPool.releaseTimeout

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

    private suspend fun createConnection(address: Address): TcpConnection {
        val conn = if (address.secure) {
            secureTcpClient.connect(address.socketAddress).await()
        } else {
            tcpClient.connect(address.socketAddress).await()
        }
        conn.startReading()
        return conn
    }
}

data class Address(val socketAddress: InetSocketAddress, val secure: Boolean)