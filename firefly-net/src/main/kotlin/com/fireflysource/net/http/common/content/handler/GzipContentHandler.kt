package com.fireflysource.net.http.common.content.handler

import com.fireflysource.common.io.ByteBufferTempInputStream
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.zip.GZIPInputStream

class GzipContentHandler<T>(
    private val handler: HttpContentHandler<T>,
    private val bufferSize: Int = 512
) : HttpContentHandler<T> {

    private val decodingBuffer = ByteArray(bufferSize)
    private val bufferInputStream = ByteBufferTempInputStream()
    private val gzipInputStream: GZIPInputStream by lazy { GZIPInputStream(bufferInputStream, bufferSize) }
    private var param: T? = null

    override fun accept(buffer: ByteBuffer, u: T) {
        param = u
        val size = buffer.remaining()
        if (size <= 0) return

        bufferInputStream.accept(buffer)
        val length = gzipInputStream.read(decodingBuffer)
        if (length <= 0) return

        val decodedBytes = ByteArray(length)
        System.arraycopy(decodingBuffer, 0, decodedBytes, 0, length)
        handler.accept(ByteBuffer.wrap(decodedBytes), u)
    }

    override fun closeFuture(): CompletableFuture<Void> {
        if (bufferInputStream.available() > 0) {
            while (true) {
                val length = gzipInputStream.read(decodingBuffer)
                if (length <= 0) break

                val decodedBytes = ByteArray(length)
                System.arraycopy(decodingBuffer, 0, decodedBytes, 0, length)
                handler.accept(ByteBuffer.wrap(decodedBytes), param)
            }
        }

        bufferInputStream.close()
        gzipInputStream.close()
        return handler.closeFuture()
    }

    override fun close() {
        closeFuture()
    }
}