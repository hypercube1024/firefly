package com.fireflysource.net.http.client.impl

import com.fireflysource.common.coroutine.event
import com.fireflysource.common.coroutine.pollAll
import com.fireflysource.common.lifecycle.AbstractLifeCycle
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.client.HttpClientConnection
import com.fireflysource.net.http.client.HttpClientConnectionManager
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.client.impl.exception.UnhandledRequestException
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.common.exception.MissingRemoteHostException
import com.fireflysource.net.http.common.exception.MissingRemotePortException
import com.fireflysource.net.http.common.model.HttpURI
import com.fireflysource.net.http.common.model.isCloseConnection
import com.fireflysource.net.tcp.TcpClientConnectionFactory
import com.fireflysource.net.tcp.TcpConnection
import com.fireflysource.net.tcp.aio.ApplicationProtocol.*
import com.fireflysource.net.tcp.aio.isSecureProtocol
import com.fireflysource.net.tcp.aio.schemaDefaultPort
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AsyncHttpClientConnectionManager(
    private val config: HttpConfig,
    private val connectionFactory: TcpClientConnectionFactory
) : HttpClientConnectionManager, AbstractLifeCycle() {

    companion object {
        private val log = SystemLogger.create(AsyncHttpClientConnectionManager::class.java)
    }

    private val connectionPoolMap = ConcurrentHashMap<Address, HttpClientConnectionPool>()

    init {
        start()
    }

    override fun send(request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        val address = buildAddress(request.uri)
        val nonPersistence = request.httpFields.isCloseConnection(request.httpVersion)
        return if (nonPersistence) sendByNonPersistenceConnection(address, request)
        else sendByPool(address, request)
    }

    override fun createHttpClientConnection(
        httpURI: HttpURI,
        supportedProtocols: List<String>
    ): CompletableFuture<HttpClientConnection> {
        return createHttpClientConnection(buildAddress(httpURI), supportedProtocols)
    }

    private fun sendByPool(address: Address, request: HttpClientRequest): CompletableFuture<HttpClientResponse> {
        val pool = connectionPoolMap.computeIfAbsent(address) { HttpClientConnectionPool(it) }
        val message = RequestMessage(request, CompletableFuture())
        pool.sendMessage(message)
        return message.response
    }

    private fun sendByNonPersistenceConnection(address: Address, request: HttpClientRequest) =
        createHttpClientConnection(address, listOf(HTTP1.value)).thenCompose { sendAndCloseConnection(it, request) }

    private fun sendAndCloseConnection(connection: HttpClientConnection, request: HttpClientRequest) =
        connection.send(request).thenCompose { response -> connection.closeFuture().thenApply { response } }


    private fun buildAddress(uri: HttpURI): Address {
        if (uri.host.isNullOrBlank()) {
            throw MissingRemoteHostException("The host is missing. uri: $uri")
        }
        val port: Int = if (uri.port > 0) {
            uri.port
        } else {
            schemaDefaultPort[uri.scheme] ?: throw MissingRemotePortException("The address port is missing. uri: $uri")
        }
        val socketAddress = InetSocketAddress(uri.host, port)
        val secure = isSecureProtocol(uri.scheme)
        return Address(socketAddress, secure)
    }

    private inner class HttpClientConnectionPool(val address: Address) : AbstractLifeCycle() {
        private var i = 0
        private val maxIndex = config.connectionPoolSize - 1
        private val httpClientConnections: Array<HttpClientConnection?> = arrayOfNulls(config.connectionPoolSize)
        private val channel: Channel<ClientRequestMessage> = Channel(Channel.UNLIMITED)
        private val checkJob = event {
            delay(TimeUnit.SECONDS.toMillis(config.checkConnectionLiveInterval))
            sendMessage(CheckConnectionLiveMessage)
        }

        init {
            start()
        }

        fun sendMessage(message: ClientRequestMessage) {
            channel.offer(message)
        }

        override fun init() {
            event {
                requestLoop@ while (true) {
                    when (val message = channel.receive()) {
                        is RequestMessage -> handleRequest(message)
                        is UnhandledRequestMessage -> processUnhandledRequestInConnection(message)
                        is CheckConnectionLiveMessage -> checkConnectionLive()
                        is StopMessage -> {
                            onStop()
                            break@requestLoop
                        }
                    }
                }
            }.invokeOnCompletion { cause ->
                if (cause != null) {
                    log.info { "The HTTP client connection pool job completion. cause: ${cause.message}" }
                }
                processUnhandledRequestInPool()
            }
        }

        override fun destroy() {
            sendMessage(StopMessage)
            checkJob.cancel()
        }

        private fun onStop() {
            httpClientConnections.forEach { it?.closeFuture() }
            processUnhandledRequestInPool()
        }

        private suspend fun handleRequest(message: RequestMessage) {
            try {
                val index = getIndex()
                val connection = getConnection(index)
                sendRequest(connection, message, index)
            } catch (ex: Throwable) {
                handleException(message, ex)
            }
        }

        private fun sendRequest(connection: HttpClientConnection, message: RequestMessage, index: Int) {
            val request = message.request
            val future = message.response
            connection.send(request).handle { response, ex ->
                if (ex != null) {
                    if (ex is UnhandledRequestException || ex.cause is UnhandledRequestException) {
                        sendMessage(UnhandledRequestMessage(request, future, connection.id, index))
                    } else {
                        handleException(message, ex)
                    }
                } else future.complete(response)
                response
            }
        }

        private suspend fun processUnhandledRequestInConnection(message: UnhandledRequestMessage) {
            val index = message.index
            val connection = getConnection(index)
            val newConnection = if (connection.id == message.connectionId) {
                createConnection(index)
            } else connection
            val newMessage = RequestMessage(message.request, message.response)
            sendRequest(newConnection, newMessage, index)
        }

        private fun handleException(message: RequestMessage, ex: Throwable) {
            val request = message.request
            val future = message.response
            if (future.isDone) return

            if (message.retry <= config.clientRetryCount) {
                val retryCount = message.retry + 1
                log.warn {
                    "HTTP client request failure. The client will retry request. " +
                            "retryCount: $retryCount, " +
                            "url: ${request.uri}, " +
                            "info:  ${ex.javaClass.name}  ${ex.message}"
                }
                sendMessage(RequestMessage(request, future, retryCount))
            } else {
                log.warn {
                    "HTTP client request failure. " +
                            "url: ${request.uri}, " +
                            "info:  ${ex.javaClass.name}  ${ex.message}"
                }
                future.completeExceptionally(ex)
            }
        }

        private fun getIndex(): Int {
            val index = if (i > maxIndex) {
                i = 0
                i
            } else i
            i++
            return index
        }

        private suspend fun getConnection(index: Int): HttpClientConnection {
            val oldConnection = httpClientConnections[index]
            return if (oldConnection != null) {
                if (oldConnection.isInvalid) createConnection(index)
                else oldConnection
            } else createConnection(index)
        }

        private suspend fun createConnection(index: Int): HttpClientConnection {
            val newConnection = createHttpClientConnection(address).await()
            httpClientConnections[index] = newConnection
            return newConnection
        }

        private suspend fun checkConnectionLive() {
            (0..maxIndex).forEach { index ->
                try {
                    getConnection(index)
                } catch (e: Exception) {
                    log.error(e) { "create http client connection failure. $address" }
                }
            }
        }

        private fun processUnhandledRequestInPool() {
            channel.pollAll { message ->
                if (message is RequestMessage) {
                    message.response.completeExceptionally(UnhandledRequestException("The HTTP client connection pool is shutdown. This request does not send."))
                }
            }
        }
    }

    private fun createHttpClientConnection(address: Address, supportedProtocols: List<String> = listOf()) =
        createTcpConnection(address, supportedProtocols).thenCompose { createHttpClientConnection(it) }

    private fun createHttpClientConnection(connection: TcpConnection): CompletableFuture<HttpClientConnection> {
        return if (connection.isSecureConnection) {
            connection.beginHandshake().thenApply { applicationProtocol ->
                when (applicationProtocol) {
                    HTTP2.value -> createHttp2ClientConnection(connection)
                    else -> createHttp1ClientConnection(connection)
                }
            }
        } else CompletableFuture.completedFuture(createHttp1ClientConnection(connection))
    }

    private fun createTcpConnection(address: Address, supportedProtocols: List<String> = listOf()) =
        connectionFactory.connect(address.socketAddress, address.secure, supportedProtocols)

    private fun createHttp1ClientConnection(connection: TcpConnection) = Http1ClientConnection(config, connection)

    private fun createHttp2ClientConnection(connection: TcpConnection) = Http2ClientConnection(config, connection)

    override fun init() {
        connectionFactory.start()
    }

    override fun destroy() {
        connectionPoolMap.values.forEach { it.stop() }
        connectionPoolMap.clear()
        connectionFactory.stop()
    }

    private data class Address(val socketAddress: InetSocketAddress, val secure: Boolean)
}

sealed class ClientRequestMessage
class RequestMessage(
    val request: HttpClientRequest, val response: CompletableFuture<HttpClientResponse>,
    val retry: Int = 0
) : ClientRequestMessage()

class UnhandledRequestMessage(
    val request: HttpClientRequest, val response: CompletableFuture<HttpClientResponse>,
    val connectionId: Int, val index: Int
) : ClientRequestMessage()

object CheckConnectionLiveMessage : ClientRequestMessage()
object StopMessage : ClientRequestMessage()