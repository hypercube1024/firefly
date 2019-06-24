package com.fireflysource.net.http.client.impl.content.handler

import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientResponse
import java.nio.ByteBuffer
import java.util.*

open class ByteBufferContentHandler : HttpClientContentHandler {

    val byteBufferList = LinkedList<ByteBuffer>()

    override fun accept(buffer: ByteBuffer, response: HttpClientResponse) {
        byteBufferList.add(buffer)
    }

}