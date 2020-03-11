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
            println(it)
            val body = BufferUtils.allocate(64)
            val pos = body.flipToFill()
            it.read(body)
            body.flipToFlush(pos)
            println("body: ${BufferUtils.toString(body)}")
        }

        assertEquals("Hello string body", handler.getPart("hello string")?.stringBody)
        assertEquals("string body 2", handler.getPart("string 2")?.stringBody)
        assertEquals("y1", handler.getPart("string 2")?.httpFields?.get("x1"))
    }

    private suspend fun createMultiPartContent(provider: MultiPartContentProvider): ByteBuffer {
        val string1 = "Hello string body"
        val string1Provider = stringBody(string1, StandardCharsets.UTF_8)
        provider.addFieldPart("hello string", string1Provider, null)

        val string2 = "string body 2"
        val string2Provider = stringBody(string2, StandardCharsets.UTF_8)
        val httpFields = HttpFields()
        httpFields.put("x1", "y1")
        provider.addFieldPart("string 2", string2Provider, httpFields)

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