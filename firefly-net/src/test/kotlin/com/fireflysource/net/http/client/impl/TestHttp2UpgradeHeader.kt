package com.fireflysource.net.http.client.impl

import com.fireflysource.common.codec.base64.Base64Utils
import com.fireflysource.net.http.client.impl.AsyncHttpClientConnectionManager.Companion.addHttp2UpgradeHeader
import com.fireflysource.net.http.client.impl.AsyncHttpClientConnectionManager.Companion.defaultSettingsFrameBytes
import com.fireflysource.net.http.client.impl.AsyncHttpClientConnectionManager.Companion.removeHttp2UpgradeHeader
import com.fireflysource.net.http.common.model.HttpHeader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * @author Pengtao Qiu
 */
class TestHttp2UpgradeHeader {

    @Test
    fun testNoConnectionHeader() {
        val request = AsyncHttpClientRequest()
        addHttp2UpgradeHeader(request)
        assertTrue(
            request.httpFields.getCSV(HttpHeader.CONNECTION, false).containsAll(
                arrayListOf(
                    "Upgrade",
                    "HTTP2-Settings"
                )
            )
        )
        assertEquals("h2c", request.httpFields[HttpHeader.UPGRADE])
        assertEquals(
            Base64Utils.encodeToString(defaultSettingsFrameBytes),
            request.httpFields[HttpHeader.HTTP2_SETTINGS]
        )

        removeHttp2UpgradeHeader(request)

        assertFalse(request.httpFields.contains(HttpHeader.HTTP2_SETTINGS))
        assertFalse(request.httpFields.contains(HttpHeader.UPGRADE))
        assertEquals("keep-alive", request.httpFields[HttpHeader.CONNECTION])
    }

    @Test
    fun test() {
        val request = AsyncHttpClientRequest()
        request.httpFields.put(HttpHeader.CONNECTION, "keep-alive")
        addHttp2UpgradeHeader(request)
        assertTrue(
            request.httpFields.getCSV(HttpHeader.CONNECTION, false).containsAll(
                arrayListOf(
                    "keep-alive",
                    "Upgrade",
                    "HTTP2-Settings"
                )
            )
        )
    }
}