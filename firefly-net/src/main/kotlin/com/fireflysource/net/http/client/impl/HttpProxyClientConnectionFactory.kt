package com.fireflysource.net.http.client.impl

import com.fireflysource.common.codec.base64.Base64
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.impl.exception.HttpTunnelHandshakeException
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.tcp.TcpClientConnectionFactory
import com.fireflysource.net.tcp.aio.ApplicationProtocol.HTTP2
import com.fireflysource.net.tcp.aio.createSecureTcpConnection
import com.fireflysource.net.tcp.aio.defaultSupportedProtocols
import com.fireflysource.net.tcp.secure.DefaultSecureEngineFactorySelector
import kotlinx.coroutines.future.await
import java.net.InetSocketAddress

class HttpProxyClientConnectionFactory(
    private val httpConfig: HttpConfig,
    private val connectionFactory: TcpClientConnectionFactory
) {

    companion object {
        private val log = SystemLogger.create(HttpProxyClientConnectionFactory::class.java)
    }

    suspend fun createHttpClientConnection(
        host: String,
        port: Int,
        secure: Boolean,
        supportedProtocols: List<String>
    ): HttpClientConnection {
        val proxyConfig = httpConfig.proxyConfig
        val address = InetSocketAddress(proxyConfig.host, proxyConfig.port)
        val proxyTcpConnection = connectionFactory.connect(address, false).await()
        val httpConnection = Http1ClientConnection(httpConfig, proxyTcpConnection)
        return if (secure) {
            try {
                val success = beginHttpTunnelHandshake(host, port, httpConnection)
                if (success) {
                    val secureEngineFactory = if (connectionFactory.secureEngineFactory == null)
                        DefaultSecureEngineFactorySelector.createSecureEngineFactory(true)
                    else connectionFactory.secureEngineFactory

                    val secureTcpConnection = createSecureTcpConnection(
                        proxyTcpConnection,
                        "",
                        0,
                        true,
                        supportedProtocols.ifEmpty { defaultSupportedProtocols },
                        secureEngineFactory
                    )
                    when (secureTcpConnection.beginHandshake().await()) {
                        HTTP2.value -> Http2ClientConnection(httpConfig, secureTcpConnection)
                        else -> Http1ClientConnection(httpConfig, secureTcpConnection)
                    }
                } else {
                    throw HttpTunnelHandshakeException("HTTP tunnel handshake failure.")
                }
            } finally {
                httpConnection.dispose()
            }
        } else httpConnection
    }

    private suspend fun beginHttpTunnelHandshake(
        host: String,
        port: Int,
        httpConnection: HttpClientConnection
    ): Boolean {
        val request = AsyncHttpClientRequest()
        request.method = HttpMethod.CONNECT.value
        request.uri = HttpURI("$host:$port")
        request.httpFields.put(HttpHeader.HOST, host)
        request.httpFields.put(HttpHeader.PROXY_CONNECTION, "keep-alive")
        val auth = httpConfig.proxyConfig.proxyAuthentication
        if (auth != null && auth.username != null && auth.password != null) {
            val authStr = Base64.encodeBase64String("${auth.username}:${auth.password}".toByteArray())
            request.httpFields.put(HttpHeader.PROXY_AUTHORIZATION, "Basic $authStr")
        }
        val response = httpConnection.send(request).await()
        log.info("HTTP tunnel handshake result: ${response.status}")
        return response.status == HttpStatus.OK_200
    }
}