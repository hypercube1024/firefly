package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.net.http.common.codec.UrlEncoded

class FormInputsContentHandler : StringContentHandler() {

    private val urlEncoded: UrlEncoded by lazy {
        val encoded = UrlEncoded()
        encoded.decode(this@FormInputsContentHandler.toString())
        encoded
    }

    fun getFormInput(name: String): String = urlEncoded.getString(name) ?: ""

    fun getFormInputs(name: String): List<String> = urlEncoded[name] ?: listOf()

    fun getFormInputs(): Map<String, List<String>> = urlEncoded
}