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

    override fun connect(address: SocketAddress): CompletableFuture<TcpConnection> =
        connect(address, listOf("h2", "http/1.1"))

    override fun connect(address: SocketAddress, supportedProtocols: List<String>): CompletableFuture<TcpConnection> =
        connect(address, "", 0, supportedProtocols)

    override fun connect(
        address: SocketAddress,
        peerHost: String,
        peerPort: Int,
        supportedProtocols: List<String>
                        ): CompletableFuture<TcpConnection> {
        val future = CompletableFuture<TcpConnection>()

        val socketChannel = AsynchronousSocketChannel.open(group)
        socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true)
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true)
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, false)
        socketChannel.connect(address, id.getAndIncrement(), object : CompletionHandler<Void?, Int> {

            override fun completed(result: Void?, connectionId: Int) {
                try {
                    val tcpConnection = if (config.enableSecureConnection) {
                        val tcpConnection = AioTcpConnection(
                            connectionId,
                            socketChannel,
                            config.timeout,
                            getMessageThread(connectionId)
                                                            )
                        val secureEngine = if (peerHost.isNotBlank() && peerPort != 0) {
                            secureEngineFactory.create(tcpConnection, true, peerHost, peerPort, supportedProtocols)
                        } else {
                            secureEngineFactory.create(tcpConnection, true, supportedProtocols)
                        }
                        AioSecureTcpConnection(tcpConnection, secureEngine, getMessageThread(connectionId))
                    } else {
                        AioTcpConnection(connectionId, socketChannel, config.timeout, getMessageThread(connectionId))
                    }
                    future.complete(tcpConnection)
                } catch (e: Exception) {
                    log.warn(e) { "connecting exception. $connectionId" }
                }
            }

            override fun failed(t: Throwable?, connectionId: Int) {
                log.warn(t) { "connecting exception. $connectionId" }
                future.completeExceptionally(t)
            }
        })
        return future
    }

    override fun getThreadName() = "aio-tcp-client"
}