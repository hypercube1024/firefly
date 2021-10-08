package com.fireflysource.net.http.client.impl

import com.fireflysource.common.`object`.Assert
import com.fireflysource.common.codec.base64.Base64
import com.fireflysource.common.string.StringUtils
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.impl.exception.HttpTunnelHandshakeException
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.tcp.TcpClientConnectionFactory
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.ApplicationProtocol
import com.fireflysource.net.tcp.aio.createSecureTcpConnection
import com.fireflysource.net.tcp.aio.defaultSupportedProtocols
import com.fireflysource.net.tcp.secure.DefaultSecureEngineFactorySelector
import kotlinx.coroutines.future.await
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture

class HttpClientConnectionFactory(
    private val httpConfig: HttpConfig,
    private val connectionFactory: TcpClientConnectionFactory
) {

    companion object {
        private val log = SystemLogger.create(HttpClientConnectionFactory::class.java)
    }

    suspend fun createHttpClientConnection(
        address: Address,
        supportedProtocols: List<String> = defaultSupportedProtocols
    ): HttpClientConnection {
        return if (isProxyEnabled()) {
            createProxyHttpClientConnection(address, supportedProtocols.ifEmpty { defaultSupportedProtocols })
        } else {
            createDirectHttpClientConnection(address, supportedProtocols.ifEmpty { defaultSupportedProtocols })
        }
    }

    private fun isProxyEnabled(): Boolean {
        return httpConfig.proxyConfig != null && StringUtils.hasText(httpConfig.proxyConfig.host) && httpConfig.proxyConfig.port > 0
    }

    private suspend fun createDirectHttpClientConnection(
        address: Address,
        supportedProtocols: List<String> = defaultSupportedProtocols
    ): HttpClientConnection {
        return connectionFactory.connect(address.socketAddress, address.secure, supportedProtocols)
            .thenCompose { createHttpClientConnection(it) }.await()
    }

    private suspend fun createProxyHttpClientConnection(
        address: Address,
        supportedProtocols: List<String> = defaultSupportedProtocols
    ): HttpClientConnection {
        val proxyAddress = getProxyAddress()
        val proxyTcpConnection = connectionFactory.connect(proxyAddress, false).await()
        val httpConnection = createHttp1ClientConnection(proxyTcpConnection)
        return if (address.secure) {
            try {
                val success =
                    beginHttpTunnelHandshake(address.socketAddress.hostName, address.socketAddress.port, httpConnection)
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
                    createHttpClientConnection(secureTcpConnection).await()
                } else {
                    throw HttpTunnelHandshakeException("HTTP tunnel handshake failure.")
                }
            } finally {
                httpConnection.dispose()
            }
        } else httpConnection
    }

    private fun getProxyAddress(): InetSocketAddress {
        val proxyConfig = httpConfig.proxyConfig
        requireNotNull(proxyConfig)
        Assert.hasText(proxyConfig.host, "The proxy host must be not null")
        Assert.isTrue(proxyConfig.port > 0, "The proxy port must be greater than 0")

        return InetSocketAddress(proxyConfig.host, proxyConfig.port)
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
        log.info("HTTP tunnel handshake result: ${response.status}, host: $host, port: $port")
        return response.status == HttpStatus.OK_200
    }

    private fun createHttpClientConnection(connection: TcpConnection): CompletableFuture<HttpClientConnection> {
        return if (connection.isSecureConnection) {
            connection.beginHandshake().thenApply { applicationProtocol ->
                when (applicationProtocol) {
                    ApplicationProtocol.HTTP2.value -> createHttp2ClientConnection(connection)
                    else -> createHttp1ClientConnection(connection)
                }
            }
        } else CompletableFuture.completedFuture(createHttp1ClientConnection(connection))
    }

    private fun createHttp1ClientConnection(connection: TcpConnection) = Http1ClientConnection(httpConfig, connection)

    private fun createHttp2ClientConnection(connection: TcpConnection) = Http2ClientConnection(httpConfig, connection)
}

data class Address(val socketAddress: InetSocketAddress, val secure: Boolean)