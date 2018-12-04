package com.fireflysource.net.tcp.aio

import com.fireflysource.common.annotation.NoArg

/**
 * @author Pengtao Qiu
 */
@NoArg
data class TcpConfig(
    var timeout: Long = 30,
    var enableSecureConnection: Boolean = false,
    var backlog: Int = 16 * 1024,
    var reuseAddr: Boolean = true,
    var keepAlive: Boolean = true,
    var tcpNoDelay: Boolean = false
                    )