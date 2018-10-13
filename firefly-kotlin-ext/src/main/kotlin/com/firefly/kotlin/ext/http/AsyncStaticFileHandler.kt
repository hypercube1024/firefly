package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.HttpHeader
import com.firefly.codec.http2.model.HttpStatus
import com.firefly.codec.http2.model.InclusiveByteRange
import com.firefly.codec.http2.model.MimeTypes
import com.firefly.kotlin.ext.nio.aRead
import com.firefly.server.http2.router.RoutingContext
import com.firefly.server.http2.router.handler.error.AbstractErrorResponseHandler
import com.firefly.server.http2.router.handler.error.DefaultErrorResponseHandlerLoader
import com.firefly.utils.CollectionUtils
import com.firefly.utils.StringUtils
import com.firefly.utils.io.BufferUtils
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.zip.GZIPOutputStream

/**
 * @author Pengtao Qiu
 */
class AsyncStaticFileHandler(
    val rootPath: String,
    val maxBufferSize: Int = 64 * 1024,
    val enableGzip: Boolean = false
                            ) : AsyncHandler {

    private val errorHandler: AbstractErrorResponseHandler = DefaultErrorResponseHandlerLoader.getInstance().handler

    override suspend fun handle(ctx: RoutingContext) {
        val path = ctx.uri.decodedPath
        val fullPath = Paths.get(rootPath, path)
        val file = fullPath.toFile()
        if (file.exists() && file.isFile) {
            // Parse the satisfiable ranges
            val reqRanges = ctx.fields.getValuesList(HttpHeader.RANGE.asString())
            val contentLength = file.length()
            val ranges = InclusiveByteRange.satisfiableRanges(reqRanges, contentLength)

            when {
                CollectionUtils.isEmpty(reqRanges) -> { // no range
                    ctx.setStatus(HttpStatus.OK_200)
                    if (enableGzip) {
                        ctx.put(HttpHeader.CONTENT_ENCODING, "gzip")
                    } else {
                        ctx.put(HttpHeader.CONTENT_LENGTH, contentLength.toString())
                    }
                    Optional.ofNullable(MimeTypes.getDefaultMimeByExtension(file.name))
                        .filter(StringUtils::hasText)
                        .ifPresent { ctx.put(HttpHeader.CONTENT_TYPE, it) }

                    val bufSize = when {
                        contentLength > maxBufferSize -> maxBufferSize
                        else -> contentLength.toInt()
                    }

                    val outputStream = if (enableGzip) {
                        GZIPOutputStream(ctx.response.outputStream)
                    } else ctx.response.outputStream

                    outputStream.use { output ->
                        AsynchronousFileChannel.open(fullPath, StandardOpenOption.READ).use { channel ->
                            var totalBytesRead = 0L
                            while (totalBytesRead < contentLength) {
                                val buf = ByteBuffer.allocate(bufSize)
                                while (buf.hasRemaining() && totalBytesRead < contentLength) {
                                    totalBytesRead += channel.aRead(buf, totalBytesRead)
                                }

                                buf.flip()
                                output.write(BufferUtils.toArray(buf))
                            }
                        }
                    }
                }
                !CollectionUtils.isEmpty(ranges) && ranges.size == 1 -> { // one range
                    val singleSatisfiableRange = ranges[0]
                    val singleLength = singleSatisfiableRange.getSize(contentLength)
                    ctx.setStatus(HttpStatus.PARTIAL_CONTENT_206)
                    ctx.put(HttpHeader.CONTENT_LENGTH, singleLength.toString())
                    ctx.put(HttpHeader.CONTENT_RANGE, singleSatisfiableRange.toHeaderRangeString(contentLength))
                    Optional.ofNullable(MimeTypes.getDefaultMimeByExtension(file.name))
                        .filter(StringUtils::hasText)
                        .ifPresent { ctx.put(HttpHeader.CONTENT_TYPE, it) }

                    val bufSize = when {
                        singleLength > maxBufferSize -> maxBufferSize
                        else -> singleLength.toInt()
                    }

                    AsynchronousFileChannel.open(fullPath).use {
                        var totalBytesRead = 0L
                        var position = singleSatisfiableRange.getFirst(contentLength)
                        var buf = ByteBuffer.allocate(bufSize)

                        while (true) {
                            val i = it.aRead(buf, position)
                            if (i < 0) {
                                break
                            }

                            totalBytesRead += i.toLong()
                            position += i.toLong()
                            buf.flip()
                            ctx.write(BufferUtils.toArray(buf))

                            if (totalBytesRead >= singleLength) {
                                break
                            }

                            buf = ByteBuffer.allocate(
                                Math.min(
                                    maxBufferSize.toLong(),
                                    singleLength - totalBytesRead
                                        ).toInt()
                                                     )
                        }
                    }
                    ctx.end()
                }
                else -> errorHandler.render(ctx, HttpStatus.RANGE_NOT_SATISFIABLE_416, null)
            }
        } else {
            errorHandler.render(ctx, HttpStatus.NOT_FOUND_404, null)
        }

    }

}