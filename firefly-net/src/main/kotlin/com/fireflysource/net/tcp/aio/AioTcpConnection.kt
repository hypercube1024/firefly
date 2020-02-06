package com.fireflysource.net.tcp.aio

import com.fireflysource.common.exception.UnsupportedOperationException
import com.fireflysource.common.sys.Result
import com.fireflysource.net.tcp.TcpConnection
import kotlinx.coroutines.CoroutineDispatcher
import java.nio.channels.AsynchronousSocketChannel
import java.util.function.Consumer

/**
 * @author Pengtao Qiu
 */
class AioTcpConnection(
    id: Int,
    maxIdleTime: Long,
    socketChannel: AsynchronousSocketChannel,
    messageThread: CoroutineDispatcher
) : AbstractAioTcpConnection(id, maxIdleTime, socketChannel, messageThread) {

    override fun isSecureConnection(): Boolean = false

    override fun isClientMode(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isHandshakeComplete(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun getSupportedApplicationProtocols(): List<String> {
        throw UnsupportedOperationException()
    }

    override fun beginHandshake(result: Consumer<Result<String>>): TcpConnection {
        throw UnsupportedOperationException()
    }
}