package com.fireflysource.net.http.server.impl.content.provider

import com.fireflysource.net.http.common.content.provider.AbstractByteBufferContentProvider
import com.fireflysource.net.http.server.HttpServerContentProvider
import java.nio.ByteBuffer

class ByteBufferContentProvider(content: ByteBuffer) : AbstractByteBufferContentProvider(content),
    HttpServerContentProvider