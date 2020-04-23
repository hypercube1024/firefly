package com.fireflysource.net.http.server.impl.content.handler

open class StringContentHandler(maxRequestBodySize: Long = 200 * 1024 * 1024) :
    ByteBufferContentHandler(maxRequestBodySize)