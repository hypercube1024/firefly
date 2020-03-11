package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.io.deleteIfExistsAsync
import com.fireflysource.common.io.flipToFill
import com.fireflysource.common.io.flipToFlush
import com.fireflysource.common.sys.Result
import com.fireflysource.common.sys.SystemLogger
import com.fireflysource.net.http.common.content.handler.AbstractByteBufferContentHandler
import com.fireflysource.net.http.common.content.handler.AbstractFileContentHandler
import com.fireflysource.net.http.common.content.provider.AbstractByteBufferContentProvider
import com.fireflysource.net.http.common.content.provider.AbstractFileContentProvider
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.MultiPart
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture

class AsyncMultiPart(
    private val maxFileSize: Long,
    private val fileSizeThreshold: Int,
    private val path: Path
) : MultiPart {

    companion object {
        private val log = SystemLogger.create(AsyncMultiPart::class.java)
    }

    private val byteBufferHandler = object : AbstractByteBufferContentHandler<Any?>() {}
    private var fileHandler: AbstractFileContentHandler<Any?>? = null
    private var fileHandlerFuture: CompletableFuture<Void>? = null
    private val fileProvider: AbstractFileContentProvider by lazy {
        object : AbstractFileContentProvider(path, StandardOpenOption.READ) {}
    }
    private val byteBufferProvider: AbstractByteBufferContentProvider by lazy {
        val size = byteBufferHandler.getByteBuffers().sumBy { it.remaining() }
        val buf = BufferUtils.allocate(size)
        val pos = buf.flipToFill()
        byteBufferHandler.getByteBuffers().forEach { BufferUtils.put(it, buf) }
        buf.flipToFlush(pos)
        object : AbstractByteBufferContentProvider(buf) {}
    }
    private val httpFields = HttpFields()
    private var size: Long = 0
    private var name: String = ""
    private var fileName: String = ""
    private var exceededFileThreshold = false

    override fun getName(): String = name

    fun setName(name: String) {
        this.name = name
    }

    override fun getFileName(): String = fileName

    fun setFileName(fileName: String) {
        this.fileName = fileName
    }

    override fun getHttpFields(): HttpFields = httpFields

    override fun getSize(): Long = size

    fun accept(item: ByteBuffer, last: Boolean) {
        size += item.remaining()
        log.debug { "Multi part accepts data. name: $name size: ${item.remaining()}, last: $last" }
        if (size > maxFileSize) {
            throw BadMessageException(HttpStatus.PAYLOAD_TOO_LARGE_413)
        }

        if (size <= fileSizeThreshold) {
            if (item.hasRemaining()) byteBufferHandler.accept(item, null)
        } else {
            if (last) {
                fileHandlerFuture = this.fileHandler?.closeFuture()
            } else {
                val fileHandler = this.fileHandler
                if (fileHandler != null) {
                    if (item.hasRemaining()) fileHandler.accept(item, null)
                } else {
                    exceededFileThreshold = true
                    val newFileHandler = object : AbstractFileContentHandler<Any?>(
                        path,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE_NEW
                    ) {}
                    this.fileHandler = newFileHandler
                    byteBufferHandler.getByteBuffers().forEach { newFileHandler.accept(it, null) }
                    if (item.hasRemaining()) newFileHandler.accept(item, null)
                }
            }
        }
    }

    suspend fun closeFileHandler() {
        fileHandler?.closeFuture()?.await()
    }

    override fun getContentType(): String = httpFields[HttpHeader.CONTENT_TYPE]

    override fun read(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        return getProvider().read(byteBuffer)
    }

    override fun getStringBody(charset: Charset): String {
        return if (exceededFileThreshold) ""
        else {
            val buffer = byteBufferProvider.toByteBuffer()
            BufferUtils.toString(buffer, charset)
        }
    }

    override fun isOpen(): Boolean {
        return getProvider().isOpen
    }

    override fun close() {
        closeFuture()
    }

    override fun closeFuture(): CompletableFuture<Void> {
        return getProvider().closeFuture()
            .thenCompose { deleteIfExistsAsync(path).asCompletableFuture() }
            .thenCompose { Result.DONE }
    }

    private fun getProvider() = if (exceededFileThreshold) fileProvider else byteBufferProvider

    override fun toString(): String {
        return "AsyncMultiPart(maxFileSize=$maxFileSize, fileSizeThreshold=$fileSizeThreshold, path=$path, httpFields=${httpFields.size()}, size=$size, name='$name', fileName='$fileName', exceededFileThreshold=$exceededFileThreshold)"
    }

}