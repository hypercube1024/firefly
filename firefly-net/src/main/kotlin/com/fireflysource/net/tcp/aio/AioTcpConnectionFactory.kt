package com.fireflysource.net.tcp.aio

import com.fireflysource.net.tcp.TcpChannelGroup
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.WrappedTcpConnection
import com.fireflysource.net.tcp.secure.SecureEngineFactory
import java.nio.channels.AsynchronousSocketChannel

fun createSecureTcpConnection(
    tcpConnection: TcpConnection,
    peerHost: String,
    peerPort: Int,
    clientMode: Boolean,
    supportedProtocols: List<String>,
    secureEngineFactory: SecureEngineFactory
): TcpConnection {
    val rawTcpConnection = if (tcpConnection is WrappedTcpConnection) {
        tcpConnection.rawTcpConnection
    } else tcpConnection
    val secureEngine = if (peerHost.isNotBlank() && peerPort != 0) {
        secureEngineFactory.create(rawTcpConnection.coroutineScope, clientMode, peerHost, peerPort, supportedProtocols)
    } else {
        secureEngineFactory.create(rawTcpConnection.coroutineScope, clientMode, supportedProtocols)
    }
    return AioSecureTcpConnection(rawTcpConnection, secureEngine)
}

fun createTcpConnection(
    connectionId: Int,
    socketChannel: AsynchronousSocketChannel,
    group: TcpChannelGroup,
    tcpConfig: TcpConfig,
    peerHost: String,
    peerPort: Int,
    clientMode: Boolean,
    supportedProtocols: List<String>,
    secureEngineFactory: SecureEngineFactory,
): TcpConnection {
    val aioTcpConnection = AioTcpConnection(
        connectionId,
        tcpConfig.timeout,
        socketChannel,
        group.getDispatcher(connectionId),
        tcpConfig.inputBufferSize
    )

    val tcpConnection = if (tcpConfig.enableSecureConnection) {
        createSecureTcpConnection(
            aioTcpConnection,
            peerHost,
            peerPort,
            clientMode,
            supportedProtocols,
            secureEngineFactory
        )
    } else aioTcpConnection

    return if (tcpConfig.enableOutputBuffer) {
        BufferedOutputTcpConnection(tcpConnection, tcpConfig.outputBufferSize)
    } else tcpConnection
}