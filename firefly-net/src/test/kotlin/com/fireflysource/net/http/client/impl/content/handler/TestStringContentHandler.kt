package com.fireflysource.net.http.client.impl.content.handler

import com.fireflysource.net.http.client.HttpClientContentHandlerFactory.stringHandler
import com.fireflysource.net.http.client.HttpClientResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.nio.ByteBuffer

class TestStringContentHandler {

    private val response = mock(HttpClientResponse::class.java)

    @Test
    fun test() {
        val handler = stringHandler()
        arrayOf(
            ByteBuffer.wrap("hello".toByteArray()),
            ByteBuffer.wrap(" buffer".toByteArray())
        ).forEach { handler.accept(it, response) }
        assertEquals("hello buffer", handler.toString())
    }
}