package com.fireflysource.net.http.common.content.handler

import java.io.InputStream
import java.util.zip.GZIPInputStream

class GzipContentHandler<T>(
    handler: HttpContentHandler<T>,
    bufferSize: Int = 512
) : AbstractCompressedContentHandler<T>(handler, bufferSize) {

    private val gzipInputStream: GZIPInputStream by lazy { GZIPInputStream(bufferInputStream, bufferSize) }

    override fun getDecodingInputStream(): InputStream = gzipInputStream

}