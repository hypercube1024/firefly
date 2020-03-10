package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.net.http.common.content.handler.AbstractByteBufferContentHandler
import com.fireflysource.net.http.common.content.handler.AbstractFileContentHandler
import com.fireflysource.net.http.common.exception.BadMessageException
import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.MultiPart
import kotlinx.coroutines.future.await
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture

class AsyncMultiPart(
    private val maxFileSize: Long,
    private val fileSizeThreshold: Int,
    private val path: Path
) : MultiPart {

    private val byteBufferHandler = object : AbstractByteBufferContentHandler<Any?>() {}
    private var fileHandler: AbstractFileContentHandler<Any?>? = null
    private var fileHandlerFuture: CompletableFuture<Void>? = null
    private val httpFields = HttpFields()
    private var size: Long = 0
    private var name: String = ""
    private var fileName: String = ""

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
        if (size > maxFileSize) {
            throw BadMessageException(HttpStatus.PAYLOAD_TOO_LARGE_413)
        }

        if (size <= fileSizeThreshold) {
            byteBufferHandler.accept(item, null)
        } else {
            if (last) {
                fileHandlerFuture = this.fileHandler?.closeFuture()
            } else {
                val fileHandler = this.fileHandler
                if (fileHandler != null) {
                    fileHandler.accept(item, null)
                } else {
                    val newFileHandler = object : AbstractFileContentHandler<Any?>(
                        path,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE_NEW
                    ) {}
                    this.fileHandler = newFileHandler
                    byteBufferHandler.getByteBuffers().forEach { newFileHandler.accept(it, null) }
                    newFileHandler.accept(item, null)
                }
            }
        }
    }

    suspend fun closeFileHandler() {
        fileHandler?.closeFuture()?.await()
    }

    override fun getContentType(): String = httpFields[HttpHeader.CONTENT_TYPE]

    override fun read(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        TODO("Not yet implemented")
    }

    override fun isOpen(): Boolean {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun closeFuture(): CompletableFuture<Void> {
        TODO("Not yet implemented")
    }

}