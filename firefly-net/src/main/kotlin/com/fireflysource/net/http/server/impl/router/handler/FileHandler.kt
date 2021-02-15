package com.fireflysource.net.http.server.impl.router.handler

import com.fireflysource.common.annotation.NoArg
import com.fireflysource.common.coroutine.asVoidFuture
import com.fireflysource.common.io.existsAsync
import com.fireflysource.common.io.readAttributesAsync
import com.fireflysource.net.http.common.codec.InclusiveByteRange
import com.fireflysource.net.http.common.codec.URIUtils
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.common.model.MimeTypes
import com.fireflysource.net.http.server.Router
import com.fireflysource.net.http.server.RoutingContext
import com.fireflysource.net.http.server.impl.content.provider.DefaultContentProvider
import com.fireflysource.net.http.server.impl.content.provider.FileContentProvider
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.CompletableFuture

class FileHandler(val config: FileConfig) : Router.Handler {

    companion object {
        fun createFileHandlerByResourcePath(path: String): FileHandler {
            val resourcePath = getResourcePath(path)
            val fileConfig = FileConfig(resourcePath)
            return FileHandler(fileConfig)
        }

        fun getResourcePath(path: String): String {
            return Optional.ofNullable(FileHandler::class.java.classLoader.getResource(path))
                .map { it.toURI() }
                .map { Paths.get(it) }
                .map { it.toString() }
                .orElse("")
        }
    }

    override fun apply(ctx: RoutingContext): CompletableFuture<Void> =
        ctx.connection.coroutineScope.launch { handleFile(ctx) }.asVoidFuture()

    private suspend fun handleFile(ctx: RoutingContext) {
        val path = URIUtils.canonicalPath(ctx.uri.decodedPath)
        val filePath = Paths.get(config.rootPath, path)
        if (!existsAsync(filePath).await()) {
            responseFileNotFound(ctx)
            return
        }

        val fileAttributes = readAttributesAsync(filePath).await()
        if (fileAttributes.isDirectory) {
            responseFileNotFound(ctx)
            return
        }

        val ranges = ctx.httpFields.getValuesList(HttpHeader.RANGE)
        if (ranges.isNullOrEmpty()) {
            responseFile(ctx, filePath)
        } else {
            val fileLength = fileAttributes.size()
            val satisfiableRanges = InclusiveByteRange.satisfiableRanges(ranges, fileLength)
            if (satisfiableRanges.isNullOrEmpty()) {
                responseRangeNotSatisfiable(ctx, fileLength)
            } else {
                if (satisfiableRanges.size == 1) {
                    val inclusiveByteRange = satisfiableRanges[0]
                    responsePartialFile(ctx, filePath, inclusiveByteRange, fileLength)
                } else {
                    responseRangeNotSatisfiable(ctx, fileLength)
                }
            }
        }
    }

    private suspend fun responseFileNotFound(ctx: RoutingContext) {
        ctx.setStatus(HttpStatus.NOT_FOUND_404)
            .setReason(HttpStatus.Code.NOT_FOUND.message)
            .contentProvider(DefaultContentProvider(HttpStatus.NOT_FOUND_404, null, ctx))
            .end()
            .await()
    }

    private suspend fun responseFile(ctx: RoutingContext, filePath: Path) {
        setContentType(ctx, filePath)

        ctx.setStatus(HttpStatus.OK_200)
            .contentProvider(FileContentProvider(filePath, StandardOpenOption.READ))
            .end()
            .await()
    }

    private suspend fun responseRangeNotSatisfiable(ctx: RoutingContext, fileLength: Long) {
        ctx.setStatus(HttpStatus.RANGE_NOT_SATISFIABLE_416)
            .put(HttpHeader.CONTENT_RANGE, InclusiveByteRange.to416HeaderRangeString(fileLength))
            .contentProvider(
                DefaultContentProvider(
                    HttpStatus.RANGE_NOT_SATISFIABLE_416,
                    RangeNotSatisfiable("The range not satisfiable"),
                    ctx
                )
            )
            .end()
            .await()
    }

    private suspend fun responsePartialFile(
        ctx: RoutingContext,
        filePath: Path,
        inclusiveByteRange: InclusiveByteRange,
        fileLength: Long
    ) {
        setContentType(ctx, filePath)

        val position = inclusiveByteRange.first
        val length = inclusiveByteRange.size
        ctx.setStatus(HttpStatus.PARTIAL_CONTENT_206)
            .put(HttpHeader.CONTENT_RANGE, inclusiveByteRange.toHeaderRangeString(fileLength))
            .contentProvider(FileContentProvider(filePath, setOf(StandardOpenOption.READ), position, length))
            .end()
            .await()
    }

    private fun setContentType(ctx: RoutingContext, filePath: Path) {
        val fileName = filePath.fileName.toString()
        val mimeType: String? = MimeTypes.getDefaultMimeByExtension(fileName)
        if (!mimeType.isNullOrBlank()) {
            ctx.put(HttpHeader.CONTENT_TYPE, mimeType)
        }
    }
}

class RangeNotSatisfiable(message: String) : IllegalArgumentException(message)

@NoArg
data class FileConfig(
    var rootPath: String
)