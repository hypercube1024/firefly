package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpHeaderValue
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.common.model.HttpURI
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TestHttp1ClientConnection {

    @Test
    @DisplayName("should add the HOST and KEEP_ALIVE headers")
    fun testPrepareHttp1Headers() {
        val request = AsyncHttpClientRequest()
        request.method = HttpMethod.GET.value
        request.uri = HttpURI("https://www.fireflysource.com/")
        prepareHttp1Headers(request)

        assertTrue(request.httpFields.getValuesList(HttpHeader.HOST.value).isNotEmpty())
        assertEquals("www.fireflysource.com", request.httpFields[HttpHeader.HOST.value])
        assertEquals(HttpHeaderValue.KEEP_ALIVE.value, request.httpFields[HttpHeader.CONNECTION.value])
    }

    @Test
    @DisplayName("should not remove user setting headers")
    fun testExistConnectionHeaders() {
        val request = AsyncHttpClientRequest()
        request.method = HttpMethod.GET.value
        request.uri = HttpURI("https://www.fireflysource.com/")
        request.httpFields.addCSV(HttpHeader.CONNECTION, HttpHeaderValue.UPGRADE.value, "HTTP2-Settings")
        prepareHttp1Headers(request)

        assertEquals(
            "${HttpHeaderValue.UPGRADE.value}, HTTP2-Settings, ${HttpHeaderValue.KEEP_ALIVE.value}",
            request.httpFields[HttpHeader.CONNECTION.value]
        )
    }
}