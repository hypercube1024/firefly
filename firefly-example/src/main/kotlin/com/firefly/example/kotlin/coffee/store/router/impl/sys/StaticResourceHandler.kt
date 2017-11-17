package com.firefly.example.kotlin.coffee.store.router.impl.sys

import com.firefly.annotation.Component
import com.firefly.codec.http2.model.HttpHeader
import com.firefly.kotlin.ext.log.KtLogger
import com.firefly.server.http2.router.Handler
import com.firefly.server.http2.router.RoutingContext
import com.firefly.server.http2.router.handler.file.StaticFileHandler
import com.firefly.utils.exception.CommonRuntimeException
import java.nio.file.Paths

/**
 * @author Pengtao Qiu
 */
@Component("staticResourceHandler")
class StaticResourceHandler : Handler {

    val staticResources = listOf("/favicon.ico", "/static/*")

    companion object {
        private val log = KtLogger.getLogger { }
        private val staticFileHandler = createStaticFileHandler()

        private fun createStaticFileHandler(): StaticFileHandler {
            try {
                val path = Paths.get(SysRouterInstaller::class.java.getResource("/").toURI())
                return StaticFileHandler(path.toAbsolutePath().toString())
            } catch (e: Exception) {
                throw CommonRuntimeException(e)
            }
        }
    }

    override fun handle(ctx: RoutingContext) {
        log.info("static file request ${ctx.uri}")
        ctx.put(HttpHeader.CACHE_CONTROL, "max-age=86400")
        staticFileHandler.handle(ctx)
    }

}