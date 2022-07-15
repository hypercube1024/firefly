package com.fireflysource.net.udp.nio

import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.AbstractConnection
import com.fireflysource.net.udp.UdpConnection
import com.fireflysource.net.udp.UdpCoroutineDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import java.nio.channels.DatagramChannel
import java.util.concurrent.TimeUnit

abstract class AbstractNioUdpConnection(
    id: Int,
    maxIdleTime: Long,
    datagramChannel: DatagramChannel,
    dispatcher: CoroutineDispatcher,
    private val nioUdpCoroutineDispatcher: UdpCoroutineDispatcher = NioUdpCoroutineDispatcher(id, dispatcher)
) : AbstractConnection(id, System.currentTimeMillis(), maxIdleTime), UdpConnection,
    UdpCoroutineDispatcher by nioUdpCoroutineDispatcher {

    companion object {
        private val log = SystemLogger.create(AbstractNioUdpConnection::class.java)
        private val timeUnit = TimeUnit.SECONDS
    }
}