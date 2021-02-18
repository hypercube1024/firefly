package com.fireflysource.net.http.server.impl.content.provider

import com.fireflysource.common.io.BufferUtils
import com.fireflysource.common.sys.ProjectVersion
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.server.HttpServerContentProvider
import com.fireflysource.net.http.server.RoutingContext
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletableFuture

class DefaultContentProvider(
    private val status: Int,
    private val exception: Throwable?,
    private val ctx: RoutingContext
) : HttpServerContentProvider {

    private val html = """
        |<!DOCTYPE html>
        |<html>
        |<head>
            |<title>${getTitle()}</title>
        |</head>
        |<body>
            |<h1>${getTitle()}</h1>
            |<p>${getContent()}</p>
            |<hr/>
            |<footer><em>powered by Firefly ${ProjectVersion.getValue()}</em></footer>
        |</body>
        |</html>
    """.trimMargin()
    private val contentByteBuffer = BufferUtils.toBuffer(html, StandardCharsets.UTF_8)
    private val provider: ByteBufferContentProvider = ByteBufferContentProvider(contentByteBuffer)

    private fun getTitle(): String {
        return "$status ${getCode().message}"
    }

    private fun getCode(): HttpStatus.Code {
        return Optional.ofNullable(HttpStatus.getCode(status)).orElse(HttpStatus.Code.INTERNAL_SERVER_ERROR)
    }

    private fun getContent(): String {
        return when (getCode()) {
            HttpStatus.Code.NOT_FOUND -> "The resource ${ctx.uri.path} is not found"
            HttpStatus.Code.INTERNAL_SERVER_ERROR -> "The server internal error. <br/> ${exception?.message}"
            else -> "${getTitle()} <br/> ${exception?.message}"
        }
    }

    override fun length(): Long = provider.length()

    override fun isOpen(): Boolean = provider.isOpen

    override fun toByteBuffer(): ByteBuffer = provider.toByteBuffer()

    override fun closeAsync(): CompletableFuture<Void> = provider.closeAsync()

    override fun close() = provider.close()

    override fun read(byteBuffer: ByteBuffer): CompletableFuture<Int> = provider.read(byteBuffer)
}