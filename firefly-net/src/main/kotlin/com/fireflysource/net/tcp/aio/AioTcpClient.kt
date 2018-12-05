package com.fireflysource.net.tcp.aio

import com.fireflysource.common.coroutine.asyncWithAttr
import com.fireflysource.common.io.aConnect
import com.fireflysource.net.tcp.TcpClient
import com.fireflysource.net.tcp.TcpConnection
import kotlinx.coroutines.future.asCompletableFuture
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.CompletableFuture

/**
 * @author Pengtao Qiu
 */
class AioTcpClient(val config: TcpConfig = TcpConfig()) : AbstractAioTcpChannelGroup(), TcpClient {

    init {
        start()
    }

    override fun enableSecureConnection(): TcpClient {
        config.enableSecureConnection = true
        return this
    }

    override fun connect(address: SocketAddress): CompletableFuture<TcpConnection> =
        asyncWithAttr(connectingThread) {
            val socketChannel = AsynchronousSocketChannel.open(group)
            socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, config.reuseAddr)
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, config.keepAlive)
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, config.tcpNoDelay)
            socketChannel.aConnect(address)
            AioTcpConnection(id.incrementAndGet(), socketChannel, config.timeout)
        }.asCompletableFuture()

    override fun getThreadName() = "aio-tcp-client"
}