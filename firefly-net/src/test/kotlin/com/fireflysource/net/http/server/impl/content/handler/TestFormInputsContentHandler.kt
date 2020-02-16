package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.net.http.common.codec.UrlEncoded
import com.fireflysource.net.http.server.RoutingContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.nio.charset.StandardCharsets

class TestFormInputsContentHandler {

    private val context = mock(RoutingContext::class.java)

    @Test
    @DisplayName("should decode web form inputs successfully")
    fun test() {
        val encoded = UrlEncoded()
        encoded["key1"] = listOf("测试1", "$%^&====")
        encoded["key2"] = listOf("v2")
        encoded.add("key3", "v3")
        encoded.add("key3", "v4")
        val string = encoded.encode(StandardCharsets.UTF_8, true)
        println(string)
        val buffer = BufferUtils.toBuffer(string, StandardCharsets.UTF_8)

        val handler = FormInputsContentHandler()
        handler.accept(buffer, context)

        assertEquals(listOf("测试1", "$%^&===="), handler.getFormInputs("key1"))
        assertEquals("v2", handler.getFormInput("key2"))
        assertEquals(listOf("v3", "v4"), handler.getFormInputs("key3"))
        assertEquals(3, handler.getFormInputs().size)
        println(handler.getFormInputs())
    }
}