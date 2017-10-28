package com.firefly.example.kotlin.coffee.store.router.impl.sys

import com.firefly.annotation.Component
import com.firefly.codec.http2.model.HttpStatus
import com.firefly.kotlin.ext.http.AsyncHandler
import com.firefly.kotlin.ext.http.asyncFail
import com.firefly.kotlin.ext.http.asyncSucceed
import com.firefly.server.http2.router.RoutingContext
import com.firefly.server.http2.router.handler.error.DefaultErrorResponseHandlerLoader

/**
 * @author Pengtao Qiu
 */
@Component("errorHandler")
class ErrorHandler : AsyncHandler {

    private val errorHandler = DefaultErrorResponseHandlerLoader.getInstance().handler

    suspend fun renderError(ctx: RoutingContext, status: Int, t: Throwable? = null) {
        errorHandler.render(ctx, status, t)
        ctx.asyncSucceed(Unit)
    }

    suspend override fun handle(ctx: RoutingContext) {
        if (ctx.hasNext()) {
            try {
                ctx.next()
            } catch (e: Exception) {
                ctx.asyncFail<Unit>(e)
            }
        } else {
            renderError(ctx, HttpStatus.NOT_FOUND_404)
        }
    }

}