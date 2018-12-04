package com.fireflysource.net.tcp.aio

import com.fireflysource.common.sys.CommonLogger
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpServer
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.function.Consumer

/**
 * @author Pengtao Qiu
 */
class AioTcpServer(val config: TcpConfig) : AbstractAioTcpChannelGroup(), TcpServer {

    companion object {
        private val log = CommonLogger.create(AioTcpServer::class.java)
    }

    private var connAcceptor: Consumer<TcpConnection> = Consumer {}

    override fun enableSecureConnection(): TcpServer {
        config.enableSecureConnection = true
        return this
    }

    override fun onAccept(consumer: Consumer<TcpConnection>): TcpServer {
        connAcceptor = consumer
        return this
    }

    override fun listen(address: SocketAddress) {
        if (isStarted) return

        start()

        try {
            val serverSocketChannel = AsynchronousServerSocketChannel.open(group)
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, config.reuseAddr)
            serverSocketChannel.bind(address, config.backlog)
            accept(serverSocketChannel)
        } catch (e: Exception) {
            log.error(e) { "bind server address exception" }
        }
    }

    private fun accept(serverSocketChannel: AsynchronousServerSocketChannel) {
        serverSocketChannel.accept(id.incrementAndGet(), object : CompletionHandler<AsynchronousSocketChannel, Int> {
            override fun completed(socketChannel: AsynchronousSocketChannel, connId: Int) {
                try {
                    connAcceptor.accept(AioTcpConnection(connId, socketChannel, config.timeout))
                } catch (e: Exception) {
                    accept(serverSocketChannel)
                    log.error(e) { "accept tcp connection exception. $connId" }

                }
            }

            override fun failed(e: Throwable, connId: Int) {
                accept(serverSocketChannel)
                log.error(e) { "accept tcp connection exception. $connId" }
            }
        })
    }

    override fun getThreadName() = "firefly-aio-tcp-server"
}