package com.fireflysource.net.http.client.impl

import com.fireflysource.net.http.client.HttpClientConnectionManager
import com.fireflysource.net.http.client.HttpClientContentProviderFactory.createStringContentProvider
import com.fireflysource.net.http.client.impl.content.provider.ByteBufferContentProvider
import com.fireflysource.net.http.client.impl.content.provider.StringContentProvider
import com.fireflysource.net.http.common.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.net.URL
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class TestAsyncHttpClientRequestBuilder {

    private val connectionManager = mock(HttpClientConnectionManager::class.java)

    @Test
    fun testPostStringBodyData() {
        val uri = HttpURI("https://www.fireflysource.com")
        val builder = AsyncHttpClientRequestBuilder(connectionManager, HttpMethod.POST.value, uri, HttpVersion.HTTP_1_1)
        builder.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN.value).body("body 123")

        val metadata = toMetaDataRequest(builder.httpRequest)
        println(metadata)
        println(metadata.fields)

        assertTrue(metadata.isRequest)

        assertNotNull(builder.httpRequest.contentProvider)
        assertTrue(builder.httpRequest.contentProvider is StringContentProvider)
    }

    @Test
    fun testPostByteBufferBodyData() {
        val uri = HttpURI("https://www.fireflysource.com")
        val builder = AsyncHttpClientRequestBuilder(connectionManager, HttpMethod.POST.value, uri, HttpVersion.HTTP_1_1)
        val buffer = ByteBuffer.allocate(20)
        builder.put(HttpHeader.CONTENT_TYPE, "application/octet-stream").body(buffer)

        val metadata = toMetaDataRequest(builder.httpRequest)
        println(metadata)
        println(metadata.fields)

        assertTrue(metadata.isRequest)

        assertNotNull(builder.httpRequest.contentProvider)
        assertTrue(builder.httpRequest.contentProvider is ByteBufferContentProvider)
    }

    @Test
    fun testPostFormData() {
        val uri = HttpURI("https://www.fireflysource.com")
        val builder = AsyncHttpClientRequestBuilder(connectionManager, HttpMethod.POST.value, uri, HttpVersion.HTTP_1_1)
        builder.putFormParam("p1", "v1")
            .putFormParam("p2", "v2")
            .addFormParam("p3", "v3")
            .addFormParam("p3", listOf("v31", "v32"))
            .putFormParam("p4", listOf())

        val metadata = toMetaDataRequest(builder.httpRequest)
        println(metadata)
        println(metadata.fields)

        assertTrue(metadata.isRequest)
        assertEquals(MimeTypes.Type.FORM_ENCODED.value, metadata.fields[HttpHeader.CONTENT_TYPE])
        assertTrue(metadata.fields[HttpHeader.CONTENT_LENGTH].toLong() > 0)

        assertNotNull(builder.httpRequest.contentProvider)
        assertTrue(builder.httpRequest.contentProvider is StringContentProvider)
        assertTrue(builder.httpRequest.contentProvider!!.length() > 0)
        assertTrue((builder.httpRequest.contentProvider as StringContentProvider).content.contains("p1=v1&p2=v2&p3=v3&p3=v31&p3=v32&p4="))

        println((builder.httpRequest.contentProvider as StringContentProvider).content)
    }

    @Test
    fun testQueryParam() {
        val uri = HttpURI("https://www.fireflysource.com")
        val builder = AsyncHttpClientRequestBuilder(connectionManager, HttpMethod.GET.value, uri, HttpVersion.HTTP_1_1)
        builder.putQueryParam("q1", "v1")
            .putQueryParam("q2", "v2")
            .addQueryParam("q2", "v22")

        val metadata = toMetaDataRequest(builder.httpRequest)
        println(metadata)

        assertTrue(metadata.isRequest)
        assertTrue(metadata.uri.query.contains("q1=v1&q2=v2&q2=v22"))

        assertEquals(0, metadata.fields.size())
    }

    @Test
    fun testQueryParam2() {
        val uri = HttpURI(URL("https://www.fireflysource.com?a1=c1&q1=v1").toURI())
        val builder = AsyncHttpClientRequestBuilder(connectionManager, HttpMethod.GET.value, uri, HttpVersion.HTTP_1_1)
        builder.putQueryParam("q1", "v1")
            .putQueryParam("q2", "v2")
            .addQueryParam("q2", "v22")
            .putQueryParam("q3", listOf("v31", "v32", "v33"))

        val metadata = toMetaDataRequest(builder.httpRequest)
        println(metadata)

        assertTrue(metadata.isRequest)
        assertTrue(metadata.uri.query.contains("a1=c1&q1=v1&q1=v1&q2=v2&q2=v22&q3=v31&q3=v32&q3=v33"))

        assertEquals(0, metadata.fields.size())
    }

    @Test
    fun testPostMultiPartData() {
        val uri = HttpURI("https://www.fireflysource.com")
        val builder = AsyncHttpClientRequestBuilder(connectionManager, HttpMethod.POST.value, uri, HttpVersion.HTTP_1_1)

        val fields = HttpFields()
        fields.add("t1", "x1")
        builder.addFieldPart("text1", createStringContentProvider("plain text1", StandardCharsets.UTF_8), fields)

        val fields2 = HttpFields()
        fields2.add("t2", "x2")
        builder.addFieldPart("text2", createStringContentProvider("plain text2", StandardCharsets.UTF_8), fields2)

        val metadata = toMetaDataRequest(builder.httpRequest)
        println(metadata)
        println(metadata.fields)

        assertTrue(metadata.isRequest)
        assertTrue(metadata.fields[HttpHeader.CONTENT_TYPE].contains("multipart/form-data"))
        assertEquals("327", metadata.fields[HttpHeader.CONTENT_LENGTH])
    }

    @Test
    fun testPostFileMultiPartData() {
        val uri = HttpURI("https://www.fireflysource.com")
        val builder = AsyncHttpClientRequestBuilder(connectionManager, HttpMethod.POST.value, uri, HttpVersion.HTTP_1_1)

        val fields = HttpFields()
        fields.add("t1", "x1")
        builder.addFilePart(
            "text1",
            "file1.txt",
            createStringContentProvider("mock file text1", StandardCharsets.UTF_8),
            fields
        )

        val metadata = toMetaDataRequest(builder.httpRequest)
        println(metadata)
        println(metadata.fields)

        assertTrue(metadata.isRequest)
        assertTrue(metadata.fields[HttpHeader.CONTENT_TYPE].contains("multipart/form-data"))
    }

    @Test
    fun testCookie() {
        val uri = HttpURI("https://www.fireflysource.com")
        val builder = AsyncHttpClientRequestBuilder(connectionManager, HttpMethod.POST.value, uri, HttpVersion.HTTP_1_1)

        builder.cookies(
            mutableListOf(
                Cookie("c1", "v1"),
                Cookie("c2", "v2"),
                Cookie("c3", "v3")
            )
        )

        val metadata = toMetaDataRequest(builder.httpRequest)
        assertTrue(metadata.isRequest)
        assertTrue(metadata.fields[HttpHeader.COOKIE].contains("c1=v1;c2=v2;c3=v3"))
    }

}