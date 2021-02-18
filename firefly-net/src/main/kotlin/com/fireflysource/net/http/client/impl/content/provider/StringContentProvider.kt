package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.net.http.client.HttpClientContentProvider
import com.fireflysource.net.http.common.content.provider.AbstractByteBufferContentProvider
import java.nio.charset.Charset

class StringContentProvider(val content: String, val charset: Charset) :
    AbstractByteBufferContentProvider(BufferUtils.toBuffer(content, charset)), HttpClientContentProvider