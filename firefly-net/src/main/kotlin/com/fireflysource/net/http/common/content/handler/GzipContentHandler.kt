package com.fireflysource.net.http.common.content.handler

import com.fireflysource.common.io.ByteBufferTempInputStream
import com.fireflysource.common.sys.Result
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.zip.GZIPInputStream

class GzipContentHandler<T>(
    private val handler: HttpContentHandler<T>,
    private val bufferSize: Int = 512
) : HttpContentHandler<T> {

    private val bufferInputStream = ByteBufferTempInputStream()
    private val gzipInputStream = GZIPInputStream(bufferInputStream, bufferSize)
    private var param: T? = null

    override fun accept(buffer: ByteBuffer, u: T) {
        param = u
        val size = buffer.remaining()
        if (size <= 0) return

        bufferInputStream.accept(buffer)
        val bytes = ByteArray(bufferSize)
        val length = gzipInputStream.read(bytes)
        if (length <= 0) return

        handler.accept(ByteBuffer.wrap(bytes), u)
    }

    override fun closeFuture(): CompletableFuture<Void> {
        this.close()
        return Result.DONE
    }

    override fun close() {
        if (bufferInputStream.available() > 0) {
            while (true) {
                val bytes = ByteArray(bufferSize)
                val length = gzipInputStream.read(bytes)
                if (length <= 0) break

                handler.accept(ByteBuffer.wrap(bytes), param)
            }
        }

        bufferInputStream.close()
        gzipInputStream.close()
    }
}