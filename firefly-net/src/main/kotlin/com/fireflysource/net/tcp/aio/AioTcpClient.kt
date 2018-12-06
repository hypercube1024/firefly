package com.fireflysource.net.tcp.aio

import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import com.fireflysource.net.tcp.secure.conscrypt.NoCheckConscryptSSLContextFactory
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.CompletableFuture


/**
 * @author Pengtao Qiu
 */
class AioTcpClient(val config: TcpConfig = TcpConfig()) : AbstractAioTcpChannelGroup(), TcpClient {

    companion object {
        private val log = SystemLogger.create(AioTcpClient::class.java)
    }

    private var secureEngineFactory: SecureEngineFactory = NoCheckConscryptSSLContextFactory()
    private var supportedProtocols: List<String> = emptyList()

    init {
        id.set(1)
        start()
    }

    override fun secureEngineFactory(secureEngineFactory: SecureEngineFactory): TcpClient {
        this.secureEngineFactory = secureEngineFactory
        return this
    }

    override fun enableSecureConnection(): TcpClient {
        config.enableSecureConnection = true
        return this
    }

    override fun connect(address: SocketAddress): CompletableFuture<TcpConnection> {
        val future = CompletableFuture<TcpConnection>()

        val socketChannel = AsynchronousSocketChannel.open(group)
        socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true)
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true)
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false)
        socketChannel.connect(address, id.getAndAdd(2), object : CompletionHandler<Void?, Int> {

            override fun completed(result: Void?, connId: Int) {
                val tcpConnection = if (config.enableSecureConnection) {
                    val conn = AioTcpConnection(connId, socketChannel, config.timeout)
                    AioSecureTcpConnection(conn, secureEngineFactory.create(conn, true, supportedProtocols))
                } else {
                    AioTcpConnection(connId, socketChannel, config.timeout)
                }
                future.complete(tcpConnection)
            }

            override fun failed(t: Throwable?, connId: Int) {
                log.warn(t) { "connect exception. $connId" }
                future.completeExceptionally(t)
            }
        })
        return future
    }

    override fun getThreadName() = "aio-tcp-client"
}