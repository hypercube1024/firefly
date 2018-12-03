package com.fireflysource.net.tcp.aio

import com.fireflysource.common.annotation.NoArg

/**
 * @author Pengtao Qiu
 */
@NoArg
data class TcpConfig(
    val timeout: Long = 30,
    val backlog: Int = 16 * 1024,
    val reuseAddr: Boolean = true,
    val keepAlive: Boolean = true,
    val tcpNoDelay: Boolean = false
                    )