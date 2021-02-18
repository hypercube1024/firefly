package com.fireflysource.net.tcp.aio

import com.fireflysource.common.annotation.NoArg
import com.fireflysource.net.tcp.exception.UnknownProtocolException

/**
 * @author Pengtao Qiu
 */
@NoArg
data class TcpConfig @JvmOverloads constructor(
    var timeout: Long = 30,
    var enableSecureConnection: Boolean = false,
    var backlog: Int = 16 * 1024,
    var reuseAddr: Boolean = true,
    var keepAlive: Boolean = true,
    var tcpNoDelay: Boolean = false,
    var inputBufferSize: Int = 16 * 1024,
    var outputBufferSize: Int = 16 * 1024,
    var enableOutputBuffer: Boolean = false
)

enum class ApplicationProtocol(val value: String) {
    HTTP2("h2"), HTTP1("http/1.1")
}

val defaultSupportedProtocols: List<String> = ApplicationProtocol.values().map { it.value }

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
        else -> throw UnknownProtocolException("Unknown protocol $scheme")
    }
}