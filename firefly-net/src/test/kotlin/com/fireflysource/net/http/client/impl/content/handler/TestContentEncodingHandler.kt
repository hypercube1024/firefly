package com.fireflysource.net.http.client.impl.content.handler

import com.fireflysource.net.http.client.HttpClientContentHandlerFactory.stringHandler
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.content.handler.GzipContentHandler
import com.fireflysource.net.http.common.content.handler.HttpContentHandler
import com.fireflysource.net.http.common.model.ContentEncoding
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.stream.Stream
import java.util.zip.GZIPOutputStream

class TestContentEncodingHandler {

    companion object {
        @JvmStatic
        fun testParametersProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.arguments(ContentEncoding.GZIP)
            )
        }
    }

    private val response = Mockito.mock(HttpClientResponse::class.java)

    private fun <T> createHandler(encoding: ContentEncoding, handler: HttpContentHandler<T>): HttpContentHandler<T> {
        return when (encoding) {
            ContentEncoding.GZIP -> GzipContentHandler<T>(handler)
            else -> throw IllegalArgumentException("")
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should decode contents successfully.")
    fun test(encoding: ContentEncoding): Unit = runBlocking {
        val stringHandler = stringHandler()
        val contentHandler = createHandler(encoding, stringHandler)

        val bytesOutputStream = ByteArrayOutputStream(128)
        val gzipOutputStream = GZIPOutputStream(bytesOutputStream)

        gzipOutputStream.use {
            gzipOutputStream.write("hello".toByteArray())
            gzipOutputStream.write(" encoding".toByteArray())
            gzipOutputStream.write(" buffer".toByteArray())
            gzipOutputStream.write(" 测试编码".toByteArray())
        }
        bytesOutputStream.close()

        arrayOf(
            ByteBuffer.wrap(bytesOutputStream.toByteArray())
        ).forEach { contentHandler.accept(it, response) }
        contentHandler.closeFuture().await()
        Assertions.assertEquals("hello encoding buffer 测试编码", stringHandler.toString())
    }
}