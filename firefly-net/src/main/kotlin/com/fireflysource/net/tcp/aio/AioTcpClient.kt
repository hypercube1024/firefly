package com.fireflysource.net.tcp.aio

import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import com.fireflysource.net.tcp.secure.conscrypt.NoCheckConscryptSSLContextFactory
import kotlinx.coroutines.CoroutineScope
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.CompletableFuture


/**
 * @author Pengtao Qiu
 */
class AioTcpClient(private val config: TcpConfig = TcpConfig()) : AbstractAioTcpChannelGroup(), TcpClient {

    companion object {
        private val log = SystemLogger.create(AioTcpClient::class.java)
    }

    private var secureEngineFactory: SecureEngineFactory = NoCheckConscryptSSLContextFactory()

    init {
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

    override fun timeout(timeout: Long): TcpClient {
        config.timeout = timeout
        return this
    }

    override fun connect(address: SocketAddress): CompletableFuture<TcpConnection> =
        connect(address, defaultSupportedProtocols)

    override fun connect(address: SocketAddress, supportedProtocols: List<String>): CompletableFuture<TcpConnection> =
        connect(address, "", 0, supportedProtocols)

    override fun connect(
        address: SocketAddress,
        peerHost: String,
        peerPort: Int,
        supportedProtocols: List<String>
    ): CompletableFuture<TcpConnection> {
        val future = CompletableFuture<TcpConnection>()
        try {
            connect(address, peerHost, peerPort, supportedProtocols, future)
        } catch (e: Exception) {
            log.warn(e) { "connecting exception. $address" }
            future.completeExceptionally(e)
        }
        return future
    }

    private fun connect(
        address: SocketAddress,
        peerHost: String,
        peerPort: Int,
        supportedProtocols: List<String>,
        future: CompletableFuture<TcpConnection>
    ) {

        fun createSecureEngine(scope: CoroutineScope) = if (peerHost.isNotBlank() && peerPort != 0) {
            secureEngineFactory.create(scope, true, peerHost, peerPort, supportedProtocols)
        } else {
            secureEngineFactory.create(scope, true, supportedProtocols)
        }

        fun createSecureConnection(connectionId: Int, socketChannel: AsynchronousSocketChannel) =
            if (config.enableSecureConnection) {
                val tcpConnection =
                    AioTcpConnection(connectionId, config.timeout, socketChannel, getDispatcher(connectionId))
                val secureEngine = createSecureEngine(tcpConnection.coroutineScope)
                AioSecureTcpConnection(tcpConnection, secureEngine)
            } else {
                AioTcpConnection(connectionId, config.timeout, socketChannel, getDispatcher(connectionId))
            }

        val socketChannel = AsynchronousSocketChannel.open(group)
        socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, config.reuseAddr)
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, config.keepAlive)
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, config.tcpNoDelay)
        socketChannel.connect(address, id.getAndIncrement(), object : CompletionHandler<Void?, Int> {

            override fun completed(result: Void?, connectionId: Int) {
                try {
                    future.complete(createSecureConnection(connectionId, socketChannel))
                } catch (e: Exception) {
                    log.warn(e) { "connecting exception. id: ${connectionId}, address: $address" }
                    future.completeExceptionally(e)
                }
            }

            override fun failed(t: Throwable?, connectionId: Int) {
                log.warn(t) { "connecting exception. id: ${connectionId}, address: $address" }
                future.completeExceptionally(t)
            }
        })
    }

    override fun getThreadName() = "aio-tcp-client"
}