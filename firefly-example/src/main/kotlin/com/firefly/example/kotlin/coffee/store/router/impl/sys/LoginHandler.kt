package com.firefly.example.kotlin.coffee.store.router.impl.sys

import com.firefly.annotation.Component
import com.firefly.kotlin.ext.http.AsyncHandler
import com.firefly.server.http2.router.RoutingContext

/**
 * @author Pengtao Qiu
 */
@Component("loginHandler")
class LoginHandler : AsyncHandler {

    override suspend fun handle(ctx: RoutingContext) {
        ctx.next() // TODO
    }

}