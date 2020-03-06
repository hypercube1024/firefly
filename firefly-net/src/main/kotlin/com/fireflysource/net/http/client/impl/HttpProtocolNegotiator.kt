package com.fireflysource.net.http.client.impl

import com.fireflysource.common.codec.base64.Base64Utils
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpHeaderValue
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.common.v2.encoder.SettingsGenerator.generateSettingsBody
import com.fireflysource.net.http.common.v2.frame.SettingsFrame

object HttpProtocolNegotiator {
    val defaultSettingsFrameBytes: ByteArray =
        BufferUtils.toArray(generateSettingsBody(SettingsFrame.DEFAULT_SETTINGS_FRAME.settings))

    fun addHttp2UpgradeHeader(request: HttpClientRequest) {
        // detect the protocol version using Connection and Upgrade HTTP headers
        val oldValues: List<String> = request.httpFields.getCSV(HttpHeader.CONNECTION, false)
        if (oldValues.isNotEmpty()) {
            val newValues = mutableListOf<String>()
            newValues.addAll(oldValues)
            newValues.add("Upgrade")
            newValues.add("HTTP2-Settings")
            request.httpFields.remove(HttpHeader.CONNECTION)
            request.httpFields.addCSV(HttpHeader.CONNECTION, *newValues.toTypedArray())
        } else {
            request.httpFields.addCSV(HttpHeader.CONNECTION, "Upgrade", "HTTP2-Settings")
        }
        request.httpFields.put(HttpHeader.UPGRADE, "h2c")

        // generate http2 settings base64
        val bytes = if (request.http2Settings.isNullOrEmpty()) {
            defaultSettingsFrameBytes
        } else {
            BufferUtils.toArray(generateSettingsBody(request.http2Settings))
        }

        val base64 = Base64Utils.encodeToUrlSafeString(bytes)
        request.httpFields.put(HttpHeader.HTTP2_SETTINGS, base64)
    }

    fun removeHttp2UpgradeHeader(request: HttpClientRequest) {
        request.httpFields.remove(HttpHeader.HTTP2_SETTINGS)
        request.httpFields.remove(HttpHeader.UPGRADE)
        request.httpFields.put(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE.value)
    }

    fun isUpgradeSuccess(response: HttpClientResponse): Boolean {
        return response.status == HttpStatus.SWITCHING_PROTOCOLS_101
    }

    fun expectUpgradeHttp2(request: HttpClientRequest): Boolean {
        return request.httpFields.contains(HttpHeader.CONNECTION, "Upgrade")
                && request.httpFields.contains(HttpHeader.UPGRADE, "h2c")
    }
}