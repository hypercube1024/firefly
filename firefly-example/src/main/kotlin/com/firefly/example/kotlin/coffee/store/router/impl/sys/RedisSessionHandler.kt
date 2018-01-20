package com.firefly.example.kotlin.coffee.store.router.impl.sys

import com.firefly.annotation.Component
import com.firefly.server.http2.router.Handler
import com.firefly.server.http2.router.RoutingContext
import com.firefly.server.http2.router.handler.session.RedisHTTPSessionHandler
import com.firefly.server.http2.router.handler.session.RedisSessionStore
import org.redisson.Redisson

/**
 * @author Pengtao Qiu
 */
@Component("redisSessionHandler")
class RedisSessionHandler : Handler {

    private val handler: RedisHTTPSessionHandler

    init {
        val store = RedisSessionStore()
        val client = Redisson.createReactive()
        store.client = client
        store.keyPrefix = "com:fireflysource"
        store.sessionKey = "test_session"
        handler = RedisHTTPSessionHandler(store)
    }

    override fun handle(ctx: RoutingContext?) = handler.handle(ctx)
}