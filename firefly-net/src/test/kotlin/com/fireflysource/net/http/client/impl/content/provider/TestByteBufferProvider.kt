package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.common.io.BufferUtils
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestByteBufferProvider {

    @Test
    fun testToByteBuffer() {
        val content = BufferUtils.allocate(12)
        val pos = BufferUtils.flipToFill(content)
        content.putInt(333)
        content.putLong(7777)
        BufferUtils.flipToFlush(content, pos)

        val provider = ByteBufferProvider(content)
        val buffer = provider.toByteBuffer()
        assertEquals(333, buffer.int)
        assertEquals(7777, buffer.long)
        assertEquals(12, provider.content.remaining())
    }

    @Test
    fun testRead() = runBlocking {
        val content = BufferUtils.allocate(12)
        val pos = BufferUtils.flipToFill(content)
        content.putInt(333)
        content.putLong(7777)
        BufferUtils.flipToFlush(content, pos)

        val provider = ByteBufferProvider(content)
        assertEquals(12, provider.length())

        val buffer = BufferUtils.allocate(6)
        val bufPos = BufferUtils.flipToFill(buffer)
        val len1 = provider.read(buffer).await()
        BufferUtils.flipToFlush(buffer, bufPos)
        assertEquals(6, len1)
        assertEquals(333, buffer.int)

        val buffer2 = BufferUtils.allocate(10)
        val bufPos2 = BufferUtils.flipToFill(buffer2)
        val len2 = provider.read(buffer2).await()
        BufferUtils.flipToFlush(buffer2, bufPos2)
        assertEquals(6, len2)

        val buffer3 = BufferUtils.allocate(8)
        val bufPos3 = BufferUtils.flipToFill(buffer3)
        buffer3.put(buffer).put(buffer2)
        BufferUtils.flipToFlush(buffer3, bufPos3)
        assertEquals(7777, buffer3.long)

        val buffer4 = BufferUtils.allocate(8)
        val bufPos4 = BufferUtils.flipToFill(buffer4)
        val len4 = provider.read(buffer2).await()
        BufferUtils.flipToFlush(buffer4, bufPos4)
        assertEquals(-1, len4)
    }
}