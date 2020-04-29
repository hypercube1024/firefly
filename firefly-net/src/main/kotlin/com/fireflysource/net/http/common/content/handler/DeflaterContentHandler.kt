package com.fireflysource.net.http.common.content.handler

import java.io.InputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

class DeflaterContentHandler<T>(
    handler: HttpContentHandler<T>,
    bufferSize: Int = 512
) : AbstractCompressedContentHandler<T>(handler, bufferSize) {

    private val inflaterInputStream: InflaterInputStream by lazy {
        InflaterInputStream(
            bufferInputStream,
            Inflater(),
            bufferSize
        )
    }

    override fun getDecodingInputStream(): InputStream = inflaterInputStream
}