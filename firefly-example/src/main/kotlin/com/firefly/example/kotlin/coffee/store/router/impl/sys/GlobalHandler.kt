package com.firefly.example.kotlin.coffee.store.router.impl.sys

import com.firefly.annotation.Component
import com.firefly.codec.http2.model.HttpStatus
import com.firefly.example.kotlin.coffee.store.vo.Response
import com.firefly.example.kotlin.coffee.store.vo.ResponseStatus
import com.firefly.kotlin.ext.http.AsyncHandler
import com.firefly.kotlin.ext.http.asyncNext
import com.firefly.kotlin.ext.log.Log
import com.firefly.server.http2.router.RoutingContext
import java.lang.IllegalArgumentException

/**
 * @author Pengtao Qiu
 */
@Component("globalHandler")
class GlobalHandler : AsyncHandler {

    private val log = Log.getLogger { }

    suspend override fun handle(ctx: RoutingContext) {
        ctx.asyncNext<Unit>(succeeded = {
            log.info("request end -> ${ctx.uri}")
            ctx.end()
        }, failed = {
            when (it) {
                is IllegalArgumentException -> {
                    ctx.setStatus(HttpStatus.OK_200)
                    ctx.writeJson(Response(ResponseStatus.ARGUMENT_ERROR.value,
                                           it.message ?: ResponseStatus.ARGUMENT_ERROR.description,
                                           false)).end()
                }
                is IllegalStateException -> {
                    ctx.setStatus(HttpStatus.OK_200)
                    ctx.writeJson(Response(ResponseStatus.SERVER_ERROR.value,
                                           it.message ?: ResponseStatus.SERVER_ERROR.description,
                                           false)).end()
                }
                else -> {
                    log.error("server exception", it)
                    ctx.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500)
                    ctx.writeJson(Response(ResponseStatus.SERVER_ERROR.value,
                                           it?.message ?: ResponseStatus.SERVER_ERROR.description,
                                           false)).end()
                }
            }
        })
    }

}