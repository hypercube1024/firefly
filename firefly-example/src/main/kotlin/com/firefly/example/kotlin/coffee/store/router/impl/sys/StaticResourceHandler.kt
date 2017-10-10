package com.firefly.example.kotlin.coffee.store.router.impl.sys

import com.firefly.annotation.Component
import com.firefly.codec.http2.model.HttpHeader
import com.firefly.kotlin.ext.http.AsyncHandler
import com.firefly.kotlin.ext.http.asyncSucceed
import com.firefly.server.http2.router.RoutingContext
import com.firefly.server.http2.router.handler.file.StaticFileHandler
import com.firefly.utils.exception.CommonRuntimeException
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import java.nio.file.Paths

/**
 * @author Pengtao Qiu
 */
@Component("staticResourceHandler")
class StaticResourceHandler : AsyncHandler {

    val staticResources = listOf("/favicon.ico", "/static/*")

    companion object {
        private val staticFileHandler = createStaticFileHandler()
        private val dispatcher = newFixedThreadPoolContext(8, "static-file-pool")

        private fun createStaticFileHandler(): StaticFileHandler {
            try {
                val path = Paths.get(SysRouterInstaller::class.java.getResource("/").toURI())
                return StaticFileHandler(path.toAbsolutePath().toString())
            } catch (e: Exception) {
                throw CommonRuntimeException(e)
            }
        }
    }

    override suspend fun handle(ctx: RoutingContext) {
        launch(dispatcher) {
            ctx.put(HttpHeader.CACHE_CONTROL, "max-age=86400")
            staticFileHandler.handle(ctx)
        }.join()
        ctx.asyncSucceed(Unit)
    }

}