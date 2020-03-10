package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.net.http.common.model.HttpFields
import com.fireflysource.net.http.server.MultiPart
import com.fireflysource.net.tcp.aio.AdaptiveBufferSize
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class AsyncMultiPart(
    private val fileSizeThreshold: Int,
    private val path: Path
) : MultiPart {

    private val httpFields = HttpFields()
    private var size: Long = 0
    private val adaptiveBufferSize = AdaptiveBufferSize()
    private var name: String = ""
    private var fileName: String = ""

    override fun getSubmittedFileName(): String {
        TODO("Not yet implemented")
    }

    override fun getName(): String = name

    fun setName(name: String) {
        this.name = name
    }

    override fun getFileName(): String = fileName

    fun setFileName(fileName: String) {
        this.fileName = fileName
    }

    override fun getHttpFields(): HttpFields = httpFields

    override fun closeFuture(): CompletableFuture<Void> {
        TODO("Not yet implemented")
    }

    override fun getSize(): Long = size

    fun addSize(delta: Long) {
        size += delta
    }

    override fun getContentType(): String {
        TODO("Not yet implemented")
    }

    override fun read(byteBuffer: ByteBuffer): CompletableFuture<Int> {
        TODO("Not yet implemented")
    }

    override fun isOpen(): Boolean {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}