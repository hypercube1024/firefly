package com.fireflysource.net.tcp.aio

import com.fireflysource.common.exception.UnsupportedOperationException
import kotlinx.coroutines.CoroutineDispatcher
import java.nio.channels.AsynchronousSocketChannel

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
}