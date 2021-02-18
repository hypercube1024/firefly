package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.net.http.common.codec.UrlEncoded
import com.fireflysource.net.http.common.model.ContentEncoding
import java.nio.charset.StandardCharsets
import java.util.*

class FormInputsContentHandler(maxRequestBodySize: Long = 200 * 1024 * 1024) :
    StringContentHandler(maxRequestBodySize) {

    private var urlEncoded: UrlEncoded? = null

    fun getFormInput(name: String, encoding: Optional<ContentEncoding> = Optional.empty()): String {
        return getUrlEncoded(encoding).getString(name) ?: ""
    }

    fun getFormInputs(name: String, encoding: Optional<ContentEncoding> = Optional.empty()): List<String> {
        return getUrlEncoded(encoding)[name] ?: listOf()
    }

    fun getFormInputs(encoding: Optional<ContentEncoding> = Optional.empty()): Map<String, List<String>> =
        getUrlEncoded(encoding)

    private fun getUrlEncoded(encoding: Optional<ContentEncoding>): UrlEncoded {
        val e = urlEncoded
        return if (e == null) {
            val encoded = UrlEncoded(this.toString(StandardCharsets.UTF_8, encoding))
            urlEncoded = encoded
            encoded
        } else e
    }
}