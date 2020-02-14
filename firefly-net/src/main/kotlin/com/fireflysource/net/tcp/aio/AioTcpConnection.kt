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
    dispatcher: CoroutineDispatcher
) : AbstractAioTcpConnection(id, maxIdleTime, socketChannel, dispatcher) {

    override fun isSecureConnection(): Boolean = false

    override fun isClientMode(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isHandshakeComplete(): Boolean = true

    override fun getSupportedApplicationProtocols(): List<String> = listOf()

    override fun beginHandshake(result: Consumer<Result<String>>): TcpConnection {
        result.accept(Result(true, "", null))
        return this
    }
}