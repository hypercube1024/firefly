package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.model.Cookie
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpVersion
import com.fireflysource.net.http.common.model.MetaData
import java.nio.ByteBuffer
import java.util.function.Supplier

class AsyncHttpClientResponse(val response: MetaData.Response) : HttpClientResponse {

    override fun getStatus(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReason(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getHttpVersion(): HttpVersion {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getHttpFields(): HttpFields {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCookies(): MutableList<Cookie> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getContentLength(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTrailerSupplier(): Supplier<HttpFields> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStringBody(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStringBody(charset: String?): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBody(): MutableList<ByteBuffer> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}