package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.io.useAwait
import com.fireflysource.net.http.client.HttpClientContentProviderFactory.stringBody
import com.fireflysource.net.http.client.impl.content.provider.MultiPartContentProvider
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.server.RoutingContext
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*

class TestMultiPartContentHandler {

    @Test
    @DisplayName("should handle multi part content successfully.")
    fun test(): Unit = runTest {
        val provider = MultiPartContentProvider()
        val buffer = createMultiPartContent(provider)
        val ctx = mockRoutingContext(provider)
        provider.closeAsync().await()
        val handler = MultiPartContentHandler()

        handler.accept(buffer, ctx)
        handler.closeAsync().await()
        handler.getParts().forEach {
            val body = BufferUtils.allocate(64)
            val pos = body.flipToFill()
            it.read(body)
            body.flipToFlush(pos)
            println(
                """
                |-------------------
                |${it.httpFields}
                |${BufferUtils.toString(body)}
                |-------------------
            """.trimMargin()
            )
        }

        assertEquals("Hello string body", handler.getPart("hello string")?.stringBody)

        val string2Part = handler.getPart("string 2")
        requireNotNull(string2Part)
        assertEquals("string body 2", string2Part.stringBody)
        assertEquals("y1", string2Part.httpFields.get("x1"))

        val filePart = handler.getPart("file body")
        requireNotNull(filePart)
        assertEquals("file body 1", filePart.stringBody)
        assertEquals("testFile.txt", filePart.fileName)
        assertEquals("g1", filePart.httpFields.get("f1"))
    }

    @Test
    @DisplayName("should get the bad message exception.")
    fun testEof(): Unit = runTest {
        val provider = MultiPartContentProvider()
        val ctx = mockRoutingContext(provider)
        val handler = MultiPartContentHandler()
        provider.closeAsync().await()

        val buf = BufferUtils.toBuffer((1..100).joinToString { "a" })
        handler.accept(buf, ctx)

        val success = try {
            handler.closeAsync().await()
            true
        } catch (e: Exception) {
            assertTrue(e is BadMessageException)
            false
        }
        assertFalse(success)
    }

    @Test
    @DisplayName("should save content to temp file successfully")
    fun testFileSizeThreshold(): Unit = runTest {
        val provider = MultiPartContentProvider()
        val buffer = createMultiPartContent(provider)
        val ctx = mockRoutingContext(provider)
        provider.closeAsync().await()
        val handler = MultiPartContentHandler(uploadFileSizeThreshold = 100)

        val buffers = LinkedList<ByteBuffer>()
        while (buffer.hasRemaining()) {
            val b = BufferUtils.allocate(32)
            val pos = b.flipToFill()
            BufferUtils.put(buffer, b)
            b.flipToFlush(pos)
            buffers.add(b)
        }

        buffers.forEach { handler.accept(it, ctx) }
        handler.closeAsync().await()

        val filePart = handler.getPart("file body")
        requireNotNull(filePart)
        assertEquals("file body 1", filePart.stringBody)
        assertEquals("testFile.txt", filePart.fileName)
        assertEquals("g1", filePart.httpFields.get("f1"))

        val bigFilePart = handler.getPart("bigFile")
        requireNotNull(bigFilePart)
        bigFilePart.useAwait {
            assertTrue(bigFilePart.stringBody.isBlank())
            val bigFileBuffer = BufferUtils.allocate(500)
            val pos = bigFileBuffer.flipToFill()
            bigFilePart.read(bigFileBuffer).await()
            bigFileBuffer.flipToFlush(pos)

            assertEquals(500, bigFileBuffer.remaining())
            val content = BufferUtils.toString(bigFileBuffer)
            assertTrue(content.contains("ccccc"))
        }
    }

    private fun mockRoutingContext(provider: MultiPartContentProvider): RoutingContext {
        val ctx = Mockito.mock(RoutingContext::class.java)
        val httpFields = HttpFields()
        httpFields.put(HttpHeader.CONTENT_TYPE, provider.contentType)
        `when`(ctx.httpFields).thenReturn(httpFields)
        return ctx
    }

    private suspend fun createMultiPartContent(provider: MultiPartContentProvider): ByteBuffer {
        val string1 = "Hello string body"
        val string1Provider = stringBody(string1, StandardCharsets.UTF_8)
        provider.addPart("hello string", string1Provider, null)

        val string2 = "string body 2"
        val string2Provider = stringBody(string2, StandardCharsets.UTF_8)
        val string2HttpFields = HttpFields()
        string2HttpFields.put("x1", "y1")
        provider.addPart("string 2", string2Provider, string2HttpFields)

        val file1 = "file body 1"
        val file1Provider = stringBody(file1, StandardCharsets.UTF_8)
        val file1HttpFields = HttpFields()
        file1HttpFields.put("f1", "g1")
        provider.addFilePart("file body", "testFile.txt", file1Provider, file1HttpFields)

        val bigFile = (1..500).joinToString(separator = "") { "c" }
        val bigFileProvider = stringBody(bigFile, StandardCharsets.UTF_8)
        provider.addFilePart("bigFile", "bigFile.txt", bigFileProvider, HttpFields())

        val buffer = BufferUtils.allocate(provider.length().toInt())
        val pos = BufferUtils.flipToFill(buffer)
        while (buffer.hasRemaining()) {
            val len = provider.read(buffer).await()
            if (len < 0) {
                break
            }
        }
        BufferUtils.flipToFlush(buffer, pos)
        return buffer
    }
}