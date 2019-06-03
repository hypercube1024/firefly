package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.common.coroutine.asyncGlobally
import com.fireflysource.common.exception.UnsupportedOperationException
import com.fireflysource.common.io.aRead
import com.fireflysource.net.http.client.HttpClientContentProvider
import kotlinx.coroutines.future.asCompletableFuture
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class FileContentProvider(val path: Path, vararg options: OpenOption) : HttpClientContentProvider {

    private val fileChannel = AsynchronousFileChannel.open(path, *options)
    private val length = Files.size(path)
    private var position: Long = 0

    override fun length(): Long = length

    override fun isOpen(): Boolean = fileChannel.isOpen

    override fun toByteBuffer(): ByteBuffer {
        throw UnsupportedOperationException("The file content does not support this method")
    }

    override fun close() {
        fileChannel.close()
    }

    override fun read(byteBuffer: ByteBuffer): CompletableFuture<Int> = asyncGlobally {
        val len = fileChannel.aRead(byteBuffer, position)
        if (len > 0) {
            position += len
        }
        len
    }.asCompletableFuture()

}