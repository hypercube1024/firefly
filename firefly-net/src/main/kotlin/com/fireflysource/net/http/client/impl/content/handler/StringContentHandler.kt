package com.fireflysource.net.http.client.impl.content.handler

import com.fireflysource.common.io.BufferUtils
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class StringContentHandler : ByteBufferContentHandler() {

    override fun toString(): String = toString(StandardCharsets.UTF_8)

    fun toString(charset: Charset): String {
        val size = byteBufferList.map { it.remaining() }.sum()
        if (size <= 0) {
            return ""
        }

        val buffer = BufferUtils.allocate(size)
        val pos = BufferUtils.flipToFill(buffer)
        byteBufferList.forEach { buffer.put(it) }
        BufferUtils.flipToFlush(buffer, pos)

        return BufferUtils.toString(buffer, charset)
    }
}