package com.fireflysource.net.http.common.content.handler

import com.fireflysource.common.io.ByteBufferInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

abstract class AbstractCompressedContentHandler<T>(
    private val handler: HttpContentHandler<T>,
    bufferSize: Int = 512
) : HttpContentHandler<T> {

    protected val bufferInputStream = ByteBufferInputStream()
    private val decodingBuffer = ByteArray(bufferSize)
    private var param: T? = null

    override fun accept(buffer: ByteBuffer, u: T) {
        param = u
        val size = buffer.remaining()
        if (size <= 0) return

        bufferInputStream.accept(buffer)
    }

    override fun closeFuture(): CompletableFuture<Void> {
        val decodingInputStream = getDecodingInputStream()
        if (bufferInputStream.available() > 0) {
            while (true) {
                val length = decodingInputStream.read(decodingBuffer)
                if (length <= 0) break

                val decodedBytes = ByteArray(length)
                System.arraycopy(decodingBuffer, 0, decodedBytes, 0, length)
                handler.accept(ByteBuffer.wrap(decodedBytes), param)
            }
        }

        bufferInputStream.close()
        decodingInputStream.close()
        return handler.closeFuture()
    }

    override fun close() {
        closeFuture()
    }


    abstract fun getDecodingInputStream(): InputStream
}