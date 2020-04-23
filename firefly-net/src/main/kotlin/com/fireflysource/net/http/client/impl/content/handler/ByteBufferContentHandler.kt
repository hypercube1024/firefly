package com.fireflysource.net.http.client.impl.content.handler

import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.content.handler.AbstractByteBufferContentHandler

open class ByteBufferContentHandler(maxRequestBodySize: Long = 200 * 1024 * 1024) :
    AbstractByteBufferContentHandler<HttpClientResponse>(maxRequestBodySize), HttpClientContentHandler