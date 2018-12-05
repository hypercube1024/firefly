package com.fireflysource.net.tcp.aio

import com.fireflysource.common.sys.CommonLogger
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpServer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.function.Consumer

/**
 * @author Pengtao Qiu
 */
class AioTcpServer(val config: TcpConfig = TcpConfig()) : AbstractAioTcpChannelGroup(), TcpServer {

    companion object {
        private val log = CommonLogger.create(AioTcpServer::class.java)
    }

    private val connChannel = Channel<TcpConnection>(UNLIMITED)
    private var connectionConsumer: Consumer<TcpConnection> = Consumer { connChannel.offer(it) }

    override fun getTcpConnectionChannel(): Channel<TcpConnection> = connChannel

    override fun enableSecureConnection(): TcpServer {
        config.enableSecureConnection = true
        return this
    }

    override fun onAccept(consumer: Consumer<TcpConnection>): TcpServer {
        connectionConsumer = consumer
        return this
    }

    override fun listen(address: SocketAddress): TcpServer {
        if (isStarted) return this

        start()

        try {
            val serverSocketChannel = AsynchronousServerSocketChannel.open(group)
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, config.reuseAddr)
            serverSocketChannel.bind(address, config.backlog)
            accept(serverSocketChannel)
        } catch (e: Exception) {
            log.error(e) { "bind server address exception" }
        }
        return this
    }

    private fun accept(serverSocketChannel: AsynchronousServerSocketChannel) {
        serverSocketChannel.accept(id.incrementAndGet(), object : CompletionHandler<AsynchronousSocketChannel, Int> {
            override fun completed(socketChannel: AsynchronousSocketChannel, connId: Int) {
                try {
                    connectionConsumer.accept(AioTcpConnection(connId, socketChannel, config.timeout))
                } catch (e: Exception) {
                    accept(serverSocketChannel)
                    log.warn(e) { "accept tcp connection exception. $connId" }
                }
            }

            override fun failed(e: Throwable, connId: Int) {
                accept(serverSocketChannel)
                log.warn(e) { "accept tcp connection exception. $connId" }
            }
        })
    }

    override fun getThreadName() = "aio-tcp-server"

    override fun destroy() {
        connChannel.close()
        super.destroy()
    }
}