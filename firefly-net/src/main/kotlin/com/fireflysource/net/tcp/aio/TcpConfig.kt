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
    var tcpNoDelay: Boolean = false,
    var bufferSize: Int = 8 * 1024,
    var enableOutputBuffer: Boolean = false
)

enum class SupportedProtocolEnum(val value: String) {
    H2("h2"), HTTP_1_1("http/1.1")
}

val defaultSupportedProtocols: List<String> = SupportedProtocolEnum.values().map { it.value }

val schemaDefaultPort = mapOf(
    "http" to 80,
    "https" to 443,
    "ws" to 80,
    "wss" to 443
)

fun isSecureProtocol(scheme: String): Boolean {
    return when (scheme) {
        "wss", "https" -> true
        "ws", "http" -> false
        else -> throw IllegalArgumentException("not support the protocol: $scheme")
    }
}