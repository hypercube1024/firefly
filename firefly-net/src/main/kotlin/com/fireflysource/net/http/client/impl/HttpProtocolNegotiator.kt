package com.fireflysource.net.http.client.impl

import com.fireflysource.common.codec.base64.Base64Utils
import com.fireflysource.common.io.BufferUtils
import com.fireflysource.net.http.client.HttpClientRequest
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpHeaderValue
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.common.v2.encoder.HeaderGenerator
import com.fireflysource.net.http.common.v2.encoder.SettingsGenerator
import com.fireflysource.net.http.common.v2.frame.SettingsFrame

object HttpProtocolNegotiator {
    private val settingsGenerator = SettingsGenerator(HeaderGenerator())
    val defaultSettingsFrameBytes: ByteArray

    init {
        defaultSettingsFrameBytes = BufferUtils.toArray(
            settingsGenerator.generateSettings(
                SettingsFrame.DEFAULT_SETTINGS_FRAME.settings,
                false
            ).byteBuffers
        )
    }

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
            BufferUtils.toArray(settingsGenerator.generateSettings(request.http2Settings, false).byteBuffers)
        }

        val base64 = Base64Utils.encodeToString(bytes)
        request.httpFields.put(HttpHeader.HTTP2_SETTINGS, base64)
    }

    fun removeHttp2UpgradeHeader(request: HttpClientRequest) {
        request.httpFields.remove(HttpHeader.HTTP2_SETTINGS)
        request.httpFields.remove(HttpHeader.UPGRADE)
        request.httpFields.put(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE.value)
    }

    fun isUpgradeSuccess(response: HttpClientResponse): Boolean {
        return response.status == HttpStatus.SWITCHING_PROTOCOLS_101
                && response.httpFields.contains(HttpHeader.CONNECTION, "Upgrade")
                && response.httpFields.contains(HttpHeader.UPGRADE, "h2c")
    }

    fun expectUpgradeHttp2(request: HttpClientRequest): Boolean {
        return request.httpFields.contains(HttpHeader.CONNECTION, "Upgrade")
                && request.httpFields.contains(HttpHeader.UPGRADE, "h2c")
    }
}