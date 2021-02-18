package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.client.HttpClient
import com.fireflysource.net.http.common.HttpConfig
import com.fireflysource.net.http.server.HttpServer
import com.fireflysource.net.http.server.HttpServerFactory
import com.fireflysource.net.tcp.aio.ApplicationProtocol
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.provider.Arguments
import java.net.InetSocketAddress
import java.util.stream.Stream
import kotlin.math.roundToLong
import kotlin.random.Random

abstract class AbstractHttpServerTestBase {

    companion object {
        @JvmStatic
        fun testParametersProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.arguments("http1", "http"),
                Arguments.arguments("http1", "https"),
                Arguments.arguments("http2", "https")
            )
        }
    }

    protected lateinit var address: InetSocketAddress

    @BeforeEach
    fun init() {
        address = InetSocketAddress("localhost", Random.nextInt(20000, 40000))
    }

    fun createHttpServer(protocol: String, schema: String, httpConfig: HttpConfig = HttpConfig()): HttpServer {
        val server = HttpServerFactory.create(httpConfig)
        when (protocol) {
            "http1" -> server.supportedProtocols(listOf(ApplicationProtocol.HTTP1.value))
            "http2" -> server.supportedProtocols(
                listOf(
                    ApplicationProtocol.HTTP2.value,
                    ApplicationProtocol.HTTP1.value
                )
            )
        }
        if (schema == "https") {
            server.enableSecureConnection()
        }
        return server
    }

    fun finish(count: Int, time: Long, httpClient: HttpClient, httpServer: HttpServer) {
        try {
            val throughput = count / (time / 1000.00)
            println("success. $time ms, ${throughput.roundToLong()} qps")
            httpClient.stop()
            httpServer.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}