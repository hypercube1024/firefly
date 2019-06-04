package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientConnectionManager
import com.fireflysource.net.http.client.impl.content.provider.StringContentProvider
import com.fireflysource.net.http.common.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class TestAsyncHttpClientRequestBuilder {

    private val connectionManager = mock(HttpClientConnectionManager::class.java)

    @Test
    fun testPostFormData() {
        val uri = HttpURI("https://www.fireflysource.com")
        val builder = AsyncHttpClientRequestBuilder(connectionManager, HttpMethod.POST.value, uri, HttpVersion.HTTP_1_1)
        builder.putFormParam("p1", "v1")
            .putFormParam("p2", "v2")
            .addFormParam("p3", "v3")

        val metadata = builder.httpRequest.toMetaDataRequest()
        println(metadata)

        assertTrue(metadata.isRequest)
        assertEquals(MimeTypes.Type.FORM_ENCODED.value, metadata.fields[HttpHeader.CONTENT_TYPE])
        assertTrue(metadata.fields[HttpHeader.CONTENT_LENGTH].toLong() > 0)

        assertNotNull(builder.httpRequest.contentProvider)
        assertTrue(builder.httpRequest.contentProvider is StringContentProvider)
        assertTrue(builder.httpRequest.contentProvider!!.length() > 0)
        assertTrue((builder.httpRequest.contentProvider as StringContentProvider).content.contains("p1=v1&p2=v2&p3=v3"))

        println((builder.httpRequest.contentProvider as StringContentProvider).content)
    }

    fun testQueryParam() {
        // TODO
    }

    fun testPostMultiPartData() {
        // TODO
    }

    fun testHttpFields() {
        // TODO
    }


}