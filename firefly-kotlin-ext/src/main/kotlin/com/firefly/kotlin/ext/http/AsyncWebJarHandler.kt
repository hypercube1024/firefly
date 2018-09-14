package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.HttpHeader
import com.firefly.codec.http2.model.HttpStatus
import com.firefly.codec.http2.model.MimeTypes
import com.firefly.server.http2.router.RoutingContext
import com.firefly.server.http2.router.handler.error.AbstractErrorResponseHandler
import com.firefly.server.http2.router.handler.error.DefaultErrorResponseHandlerLoader
import com.firefly.utils.StringUtils
import com.firefly.utils.lang.URIUtils
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.IO
import kotlinx.coroutines.experimental.withContext
import java.util.*
import java.util.zip.GZIPOutputStream

/**
 * @author Pengtao Qiu
 */
class AsyncWebJarHandler(val rootPath: String,
                         val enableGzip: Boolean) : AsyncHandler {

    private val errorHandler: AbstractErrorResponseHandler = DefaultErrorResponseHandlerLoader.getInstance().handler

    constructor(enableGzip: Boolean = false) : this("/META-INF/resources", enableGzip)

    override suspend fun handle(ctx: RoutingContext) = withContext(Dispatchers.IO) {
        val path = URIUtils.canonicalPath(ctx.uri.path)
        try {
            val url = AsyncWebJarHandler::class.java.getResource(rootPath + path)
            if (url == null) {
                errorHandler.render(ctx, HttpStatus.NOT_FOUND_404, null)
            } else {
                val data = url.readBytes()

                ctx.setStatus(HttpStatus.OK_200)
                Optional.ofNullable(MimeTypes.getDefaultMimeByExtension(path))
                        .filter(StringUtils::hasText)
                        .ifPresent { ctx.put(HttpHeader.CONTENT_TYPE, it) }

                if (enableGzip) {
                    ctx.put(HttpHeader.CONTENT_ENCODING, "gzip")
                } else {
                    ctx.put(HttpHeader.CONTENT_LENGTH, data.size.toString())
                }

                val outputStream = if (enableGzip) {
                    GZIPOutputStream(ctx.response.outputStream)
                } else ctx.response.outputStream

                outputStream.use { it.write(data) }
            }
        } catch (e: Exception) {
            errorHandler.render(ctx, HttpStatus.INTERNAL_SERVER_ERROR_500, e)
        }
        Unit
    }

}