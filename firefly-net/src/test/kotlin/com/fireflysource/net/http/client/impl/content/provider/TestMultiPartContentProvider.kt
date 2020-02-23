package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.net.http.client.HttpClientContentProviderFactory.stringBody
import com.fireflysource.net.http.common.model.HttpFields
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class TestMultiPartContentProvider {

    @Test
    @DisplayName("should generate multi-part format successfully")
    fun testRead() = runBlocking {
        val provider = MultiPartContentProvider()

        val str = "Hello string body"
        val strProvider = stringBody(str, StandardCharsets.UTF_8)
        provider.addFieldPart("hello string", strProvider, null)

        val str2 = "string body 2"
        val strProvider2 = stringBody(str2, StandardCharsets.UTF_8)
        val httpFields = HttpFields()
        httpFields.put("x1", "y1")
        provider.addFieldPart("string 2", strProvider2, httpFields)

        val buffer = BufferUtils.allocate(provider.length().toInt())
        val pos = BufferUtils.flipToFill(buffer)
        while (buffer.hasRemaining()) {
            val len = provider.read(buffer).await()
            if (len < 0) {
                break
            }
        }
        BufferUtils.flipToFlush(buffer, pos)
        println()

        println("Content-Type: ${provider.contentType}")
        println()

        val content = BufferUtils.toUTF8String(buffer)
        println(content)
        assertTrue(content.contains("Hello string body"))
        assertTrue(content.contains("string body 2"))
        assertTrue(content.contains("x1: y1"))
    }
}