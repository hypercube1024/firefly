package com.fireflysource.net.tcp.aio

import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.TcpChannelGroup
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.secure.DefaultSecureEngineFactorySelector
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.CompletableFuture


/**
 * @author Pengtao Qiu
 */
class AioTcpClient(private val config: TcpConfig = TcpConfig()) : AbstractLifeCycle(), TcpClient {

    companion object {
        private val log = SystemLogger.create(AioTcpClient::class.java)
    }

    private var secureEngineFactory: SecureEngineFactory =
        DefaultSecureEngineFactorySelector.createSecureEngineFactory(true)
    private var group: TcpChannelGroup = AioTcpChannelGroup("aio-tcp-client")
    private var stopGroup = true

    override fun init() {
        group.start()
    }

    override fun destroy() {
        if (stopGroup) group.stop()
    }

    override fun tcpChannelGroup(group: TcpChannelGroup): TcpClient {
        this.group = group
        return this
    }

    override fun stopTcpChannelGroup(stop: Boolean): TcpClient {
        this.stopGroup = stop
        return this
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

    override fun bufferSize(bufferSize: Int): TcpClient {
        config.outputBufferSize = bufferSize
        return this
    }

    override fun enableOutputBuffer(): TcpClient {
        config.enableOutputBuffer = true
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
        start()

        try {
            val socketChannel = AsynchronousSocketChannel.open(group.asynchronousChannelGroup)
            socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, config.reuseAddr)
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, config.keepAlive)
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, config.tcpNoDelay)
            socketChannel.connect(address, group.nextId, object : CompletionHandler<Void?, Int> {

                override fun completed(result: Void?, connectionId: Int) {
                    try {
                        future.complete(
                            createTcpConnection(
                                connectionId,
                                socketChannel,
                                group,
                                config,
                                peerHost,
                                peerPort,
                                true,
                                supportedProtocols,
                                secureEngineFactory
                            )
                        )
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
        } catch (e: Exception) {
            log.error(e) { "TCP client connect exception" }
            future.completeExceptionally(e)
        }
    }

}

fun TcpClient.connectAsync(host: String, port: Int, block: suspend CoroutineScope.(TcpConnection) -> Unit): TcpClient {
    connect(host, port).thenAccept { connection -> connection.coroutineScope.launch { block(connection) } }
    return this
}