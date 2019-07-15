package com.fireflysource.net.http.client.impl.content.handler

import com.fireflysource.common.io.BufferUtils
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class StringContentHandler : ByteBufferContentHandler() {

    private val utf8String: String by lazy { toString(StandardCharsets.UTF_8) }

    override fun toString(): String = utf8String

    fun toString(charset: Charset): String {
        val size = byteBufferList.map { it.remaining() }.sum()
        if (size <= 0) {
            return ""
        }

        val buffer = BufferUtils.allocate(size)
        val pos = BufferUtils.flipToFill(buffer)
        byteBufferList.forEach {
            buffer.put(it)
            it.flip()
        }
        BufferUtils.flipToFlush(buffer, pos)

        return BufferUtils.toString(buffer, charset)
    }
}