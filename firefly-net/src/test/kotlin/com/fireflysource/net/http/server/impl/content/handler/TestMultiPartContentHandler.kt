package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.net.http.client.HttpClientContentProviderFactory.stringBody
import com.fireflysource.net.http.client.impl.content.provider.MultiPartContentProvider
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.server.RoutingContext
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class TestMultiPartContentHandler {

    @Test
    @DisplayName("should handle multi part content successfully.")
    fun test(): Unit = runBlocking {
        val provider = MultiPartContentProvider()
        val buffer = createMultiPartContent(provider)
        val handler = MultiPartContentHandler()

        val ctx = Mockito.mock(RoutingContext::class.java)
        val httpFields = HttpFields()
        httpFields.put(HttpHeader.CONTENT_TYPE, provider.contentType)
        `when`(ctx.httpFields).thenReturn(httpFields)

        handler.accept(buffer, ctx)
        handler.closeFuture().await()
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

    private suspend fun createMultiPartContent(provider: MultiPartContentProvider): ByteBuffer {
        val string1 = "Hello string body"
        val string1Provider = stringBody(string1, StandardCharsets.UTF_8)
        provider.addFieldPart("hello string", string1Provider, null)

        val string2 = "string body 2"
        val string2Provider = stringBody(string2, StandardCharsets.UTF_8)
        val string2HttpFields = HttpFields()
        string2HttpFields.put("x1", "y1")
        provider.addFieldPart("string 2", string2Provider, string2HttpFields)

        val file1 = "file body 1"
        val file1Provider = stringBody(file1, StandardCharsets.UTF_8)
        val file1HttpFields = HttpFields()
        file1HttpFields.put("f1", "g1")
        provider.addFilePart("file body", "testFile.txt", file1Provider, file1HttpFields)

        val buffer = BufferUtils.allocate(provider.length().toInt())
        val pos = BufferUtils.flipToFill(buffer)
        while (buffer.hasRemaining()) {
            val len = provider.read(buffer).await()
            if (len < 0) {
                break
            }
        }
        BufferUtils.flipToFlush(buffer, pos)
//        println(BufferUtils.toString(buffer))
        return buffer
    }
}