package com.fireflysource.net.tcp.aio

import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.tcp.TcpChannelGroup
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.TcpServer
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import com.fireflysource.net.tcp.secure.jdk.SelfSignedCredentialOpenJdkSSLContextFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.*
import java.util.function.Consumer

/**
 * @author Pengtao Qiu
 */
class AioTcpServer(private val config: TcpConfig = TcpConfig()) : AbstractLifeCycle(), TcpServer {

    companion object {
        private val log = SystemLogger.create(AioTcpServer::class.java)
    }

    private var group: TcpChannelGroup = AioTcpChannelGroup("aio-tcp-server")
    private val connectionChannel = Channel<TcpConnection>(UNLIMITED)
    private var connectionConsumer: Consumer<TcpConnection> = Consumer { connectionChannel.offer(it) }
    private var secureEngineFactory: SecureEngineFactory =
        SelfSignedCredentialOpenJdkSSLContextFactory()
    private var supportedProtocols: List<String> = defaultSupportedProtocols
    private var peerHost: String = ""
    private var peerPort: Int = 0
    private var serverSocketChannel: AsynchronousServerSocketChannel? = null
    private val acceptSocketConnectionCompletionHandler =
        object : CompletionHandler<AsynchronousSocketChannel, Int> {
            override fun completed(socketChannel: AsynchronousSocketChannel, connectionId: Int) {
                onAcceptCompleted(socketChannel, connectionId)
            }

            override fun failed(e: Throwable, connectionId: Int) {
                onAcceptFailed(e, connectionId)
            }
        }

    override fun init() {
        group.start()
    }

    override fun destroy() {
        try {
            serverSocketChannel?.close()
        } catch (e: Exception) {
            log.error(e) { "close server socket channel exception" }
        }
        group.stop()
    }

    override fun tcpChannelGroup(group: TcpChannelGroup): TcpServer {
        this.group = group
        return this
    }

    override fun getTcpConnectionChannel(): Channel<TcpConnection> = connectionChannel

    override fun secureEngineFactory(secureEngineFactory: SecureEngineFactory): TcpServer {
        this.secureEngineFactory = secureEngineFactory
        return this
    }

    override fun supportedProtocols(supportedProtocols: List<String>): TcpServer {
        this.supportedProtocols = supportedProtocols
        return this
    }

    override fun peerHost(peerHost: String): TcpServer {
        this.peerHost = peerHost
        return this
    }

    override fun peerPort(peerPort: Int): TcpServer {
        this.peerPort = peerPort
        return this
    }

    override fun enableSecureConnection(): TcpServer {
        config.enableSecureConnection = true
        return this
    }

    override fun timeout(timeout: Long): TcpServer {
        config.timeout = timeout
        return this
    }

    override fun bufferSize(bufferSize: Int): TcpServer {
        config.outputBufferSize = bufferSize
        return this
    }

    override fun enableOutputBuffer(): TcpServer {
        config.enableOutputBuffer = true
        return this
    }

    override fun onAccept(consumer: Consumer<TcpConnection>): TcpServer {
        connectionConsumer = consumer
        return this
    }

    override fun listen(address: SocketAddress): TcpServer {
        if (isStarted) {
            return this
        }

        start()

        try {
            val socketChannel = AsynchronousServerSocketChannel.open(group.asynchronousChannelGroup)
            socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, config.reuseAddr)
            socketChannel.bind(address, config.backlog)
            this.serverSocketChannel = socketChannel
            accept()
        } catch (e: Exception) {
            log.error(e) { "bind server address exception" }
        }
        return this
    }

    private fun accept() {
        try {
            serverSocketChannel?.accept(group.nextId, acceptSocketConnectionCompletionHandler)
        } catch (e: ShutdownChannelGroupException) {
            log.info { "the channel group is shutdown." }
        } catch (e: Exception) {
            log.error(e) { "accept socket channel exception." }
        }
    }

    private fun onAcceptCompleted(socketChannel: AsynchronousSocketChannel, connectionId: Int) {
        fun createSecureEngine(scope: CoroutineScope) = if (peerHost.isNotBlank() && peerPort != 0) {
            secureEngineFactory.create(scope, false, peerHost, peerPort, supportedProtocols)
        } else {
            secureEngineFactory.create(scope, false, supportedProtocols)
        }

        try {
            socketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, config.reuseAddr)
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, config.keepAlive)
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, config.tcpNoDelay)
            val aioTcpConnection = AioTcpConnection(
                connectionId, config.timeout,
                socketChannel, group.getDispatcher(connectionId), config.inputBufferSize
            )

            val tcpConnection = if (config.enableSecureConnection) {
                val secureEngine = createSecureEngine(aioTcpConnection.coroutineScope)
                AioSecureTcpConnection(aioTcpConnection, secureEngine)
            } else aioTcpConnection

            val connection = if (config.enableOutputBuffer) {
                BufferedOutputTcpConnection(tcpConnection, config.outputBufferSize)
            } else tcpConnection

            connectionConsumer.accept(connection)
            log.debug { "accept the client connection. $connectionId" }
        } catch (e: Exception) {
            log.warn(e) { "accept connection exception. $connectionId" }
        } finally {
            accept()
        }
    }

    private fun onAcceptFailed(e: Throwable, connectionId: Int) {
        when (e) {
            is ClosedChannelException -> {
                log.info { "The server socket channel has been closed." }
            }
            is ShutdownChannelGroupException -> {
                log.info { "the server is shutdown. stop to accept connection." }
            }
            else -> {
                log.warn(e) { "accept connection failure. $connectionId" }
                accept()
            }
        }
    }

}