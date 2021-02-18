package com.fireflysource.net.http.client.impl.content.handler

class StringContentHandler(maxRequestBodySize: Long = 200 * 1024 * 1024) : ByteBufferContentHandler(maxRequestBodySize)