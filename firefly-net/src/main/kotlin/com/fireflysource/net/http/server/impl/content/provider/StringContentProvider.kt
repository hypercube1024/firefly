package com.fireflysource.net.http.server.impl.content.provider

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.net.http.common.content.provider.AbstractByteBufferContentProvider
import com.fireflysource.net.http.server.HttpServerContentProvider
import java.nio.charset.Charset

class StringContentProvider(val content: String, val charset: Charset) :
    AbstractByteBufferContentProvider(BufferUtils.toBuffer(content, charset)), HttpServerContentProvider