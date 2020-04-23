package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.net.http.common.content.handler.AbstractByteBufferContentHandler
import com.fireflysource.net.http.server.HttpServerContentHandler
import com.fireflysource.net.http.server.RoutingContext

open class ByteBufferContentHandler(maxRequestBodySize: Long = 200 * 1024 * 1024) :
    AbstractByteBufferContentHandler<RoutingContext>(maxRequestBodySize), HttpServerContentHandler