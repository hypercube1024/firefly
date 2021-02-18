package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.net.http.client.HttpClientContentProvider
import com.fireflysource.net.http.common.content.provider.AbstractByteBufferContentProvider
import java.nio.ByteBuffer

open class ByteBufferContentProvider(content: ByteBuffer) :
    AbstractByteBufferContentProvider(content), HttpClientContentProvider