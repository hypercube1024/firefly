package com.fireflysource.net.http.client.impl.content.handler

import com.fireflysource.net.http.client.HttpClientContentHandlerFactory.stringHandler
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.content.handler.DeflaterContentHandler
import com.fireflysource.net.http.common.content.handler.GzipContentHandler
import com.fireflysource.net.http.common.content.handler.HttpContentHandler
import com.fireflysource.net.http.common.model.ContentEncoding
import com.fireflysource.net.http.common.model.ContentEncoding.DEFLATE
import com.fireflysource.net.http.common.model.ContentEncoding.GZIP
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.stream.Stream
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

class TestCompressedContentHandler {

    companion object {
        @JvmStatic
        fun testParametersProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.arguments(GZIP),
                Arguments.arguments(DEFLATE)
            )
        }
    }

    private val response = Mockito.mock(HttpClientResponse::class.java)

    private fun <T> createHandler(encoding: ContentEncoding, handler: HttpContentHandler<T>): HttpContentHandler<T> {
        return when (encoding) {
            GZIP -> GzipContentHandler<T>(handler)
            DEFLATE -> DeflaterContentHandler<T>(handler)
            else -> throw IllegalArgumentException("")
        }
    }

    private fun createEncodingOutputStream(encoding: ContentEncoding, outputStream: OutputStream): OutputStream {
        return when (encoding) {
            GZIP -> GZIPOutputStream(outputStream)
            DEFLATE -> DeflaterOutputStream(outputStream)
            else -> throw IllegalArgumentException("")
        }
    }

    private fun createTestData(encoding: ContentEncoding): List<ByteBuffer> {
        val bytesOutputStream = ByteArrayOutputStream(512)

        val encodingOutputStream = createEncodingOutputStream(encoding, bytesOutputStream)
        encodingOutputStream.use {
            it.write("hello".toByteArray())
            it.write(" encoding".toByteArray())
            it.write(" buffer".toByteArray())
            it.write(" 测试编码".toByteArray())
        }

        val list = LinkedList<ByteBuffer>()
        val bytesInputStream = ByteArrayInputStream(bytesOutputStream.toByteArray())
        bytesInputStream.use {
            while (true) {
                val bytes = ByteArray(4)
                val len = it.read(bytes)
                if (len < 0) break
                list.add(ByteBuffer.wrap(bytes, 0, len))
            }
        }

        bytesOutputStream.close()
        return list
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should decode contents successfully.")
    fun test(encoding: ContentEncoding): Unit = runBlocking {
        val stringHandler = stringHandler()
        val contentHandler = createHandler(encoding, stringHandler)
        val data = createTestData(encoding)

        data.forEach { contentHandler.accept(it, response) }
        contentHandler.closeFuture().await()
        Assertions.assertEquals("hello encoding buffer 测试编码", stringHandler.toString())
    }
}