package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.net.http.client.HttpClientContentProviderFactory.createStringContentProvider
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class TestStringContentProvider {

    @Test
    @DisplayName("should get string successfully")
    fun testToByteBuffer() {
        val str = "Hello string body"
        val provider = createStringContentProvider(str, StandardCharsets.UTF_8)
        val byteBuffer = provider.toByteBuffer()
        assertEquals(str, BufferUtils.toString(byteBuffer, StandardCharsets.UTF_8))
    }

    @Test
    @DisplayName("should read string successfully")
    fun testRead() = runBlocking {
        val str = "Hello string body"
        val provider = createStringContentProvider(str, StandardCharsets.UTF_8)

        val byteBuffer = BufferUtils.allocate(5)
        val pos = BufferUtils.flipToFill(byteBuffer)
        val len = provider.read(byteBuffer).await()
        BufferUtils.flipToFlush(byteBuffer, pos)

        assertEquals(5, len)
        assertEquals(5, byteBuffer.remaining())
        assertEquals("Hello", BufferUtils.toString(byteBuffer, StandardCharsets.UTF_8))

        val byteBuffer2 = BufferUtils.allocate(20)
        val pos2 = BufferUtils.flipToFill(byteBuffer2)
        val len2 = provider.read(byteBuffer2).await()
        BufferUtils.flipToFlush(byteBuffer2, pos2)

        assertEquals(str.length - 5, len2)
        assertEquals(str.length - 5, byteBuffer2.remaining())
        assertEquals(" string body", BufferUtils.toString(byteBuffer2, StandardCharsets.UTF_8))

        val byteBuffer3 = BufferUtils.allocate(10)
        val pos3 = BufferUtils.flipToFill(byteBuffer3)
        val len3 = provider.read(byteBuffer3).await()
        BufferUtils.flipToFlush(byteBuffer3, pos3)

        assertEquals(-1, len3)
        assertEquals(0, byteBuffer3.remaining())
    }
}