package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.HttpHeader
import com.firefly.codec.http2.model.HttpStatus
import com.firefly.codec.http2.model.MimeTypes
import com.firefly.server.http2.router.RoutingContext
import com.firefly.server.http2.router.handler.error.AbstractErrorResponseHandler
import com.firefly.server.http2.router.handler.error.DefaultErrorResponseHandlerLoader
import com.firefly.utils.StringUtils
import com.firefly.utils.io.BufferUtils
import com.firefly.utils.lang.URIUtils
import kotlinx.coroutines.experimental.nio.aRead
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Paths
import java.util.*

/**
 * @author Pengtao Qiu
 */
class AsyncStaticFileHandler(val rootPath: String) : AsyncHandler {

    private val errorHandler: AbstractErrorResponseHandler = DefaultErrorResponseHandlerLoader.getInstance().handler

    override suspend fun handle(ctx: RoutingContext) {
        val path = URIUtils.canonicalPath(ctx.uri.path)
        val fullPath = Paths.get(rootPath, path)
        val file = fullPath.toFile()
        if (file.exists() && file.isFile) {
            AsynchronousFileChannel.open(fullPath).use {
                val length = it.size()
                var totalBytesRead = 0L

                ctx.setStatus(HttpStatus.OK_200)
                ctx.put(HttpHeader.CONTENT_LENGTH, length.toString())
                Optional.ofNullable(MimeTypes.getDefaultMimeByExtension(file.name))
                        .filter(StringUtils::hasText)
                        .ifPresent { ctx.put(HttpHeader.CONTENT_TYPE, it) }

                while (totalBytesRead < length) {
                    val buf = ByteBuffer.allocate(4096)
                    while (buf.hasRemaining() && totalBytesRead < length) {
                        totalBytesRead += it.aRead(buf, totalBytesRead)
                    }

                    buf.flip()
                    ctx.write(BufferUtils.toArray(buf))
                }
            }
            ctx.end()
        } else {
            errorHandler.render(ctx, HttpStatus.NOT_FOUND_404, null)
        }

    }

}