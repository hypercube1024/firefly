package com.fireflysource.net.http.client.impl

import com.fireflysource.common.codec.base64.Base64Utils
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.addHttp2UpgradeHeader
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.defaultSettingsFrameBytes
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.expectUpgradeHttp2
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.isUpgradeSuccess
import com.fireflysource.net.http.client.impl.HttpProtocolNegotiator.removeHttp2UpgradeHeader
import com.fireflysource.net.http.common.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * @author Pengtao Qiu
 */
class TestHttpProtocolNegotiator {

    @Test
    @DisplayName("should remove h2c headers successfully")
    fun testRemoveHttp2UpgradeHeader() {
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
        assertFalse(expectUpgradeHttp2(request))
        assertFalse(request.httpFields.contains(HttpHeader.HTTP2_SETTINGS))
        assertFalse(request.httpFields.contains(HttpHeader.UPGRADE))
        assertEquals("keep-alive", request.httpFields[HttpHeader.CONNECTION])
    }

    @Test
    @DisplayName("should add h2c headers successfully")
    fun testAddHttp2UpgradeHeader() {
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
        assertTrue(expectUpgradeHttp2(request))
    }

    @Test
    @DisplayName("should check response upgrade http2 successfully")
    fun testUpgradeSuccess() {
        val metadata = MetaData.Response(HttpVersion.HTTP_1_1, HttpStatus.SWITCHING_PROTOCOLS_101, HttpFields())
        metadata.fields.put(HttpHeader.CONNECTION, "Upgrade")
        metadata.fields.put(HttpHeader.UPGRADE, "h2c")
        val response = AsyncHttpClientResponse(metadata, null)
        assertTrue(isUpgradeSuccess(response))
    }

}