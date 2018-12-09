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
    socketChannel: AsynchronousSocketChannel,
    timeout: Long,
    messageThread: CoroutineDispatcher
                      ) : AbstractTcpConnection(id, socketChannel, timeout, messageThread) {

    override fun isSecureConnection(): Boolean = false

    override fun isClientMode(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isHandshakeFinished(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun getSupportedApplicationProtocols(): List<String> {
        throw UnsupportedOperationException()
    }

    override fun onHandshakeFinished(result: Consumer<Result<String>>): TcpConnection {
        throw UnsupportedOperationException()
    }
}