package com.firefly.example.kotlin.coffee.store.router.impl.sys

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.example.kotlin.coffee.store.router.RouterInstaller
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.server.http2.router.RouterManager.DEFAULT_LAST_ROUTER_ID
import com.firefly.server.http2.router.handler.session.LocalHTTPSessionHandler

/**
 * @author Pengtao Qiu
 */
@Component("sysRouterInstaller")
class SysRouterInstaller : RouterInstaller {

    @Inject
    private lateinit var server: HttpServer

    @Inject
    private lateinit var globalHandler: GlobalHandler

    @Inject
    private lateinit var localHTTPSessionHandler: LocalHTTPSessionHandler

    @Inject
    private lateinit var redisHTTPSessionHandler: RedisSessionHandler

    @Inject
    private lateinit var staticResourceHandler: StaticResourceHandler

    @Inject
    private lateinit var loginHandler: LoginHandler

    @Inject
    private lateinit var transactionalHandler: TransactionalHandler

    @Inject
    private lateinit var errorHandler: ErrorHandler

    override fun install() = server.addRouters {

        // static file handler
        router {
            httpMethod = HttpMethod.GET
            paths = staticResourceHandler.staticResources
            asyncHandler(staticResourceHandler)
        }

        // global handler
        router {
            path = "*"
            asyncHandler(globalHandler)
        }

        // session handler
        router {
            path = "*"
            asyncHandler { redisHTTPSessionHandler.handle(this) }
        }

        // login handler
        router {
            path = "*"
            asyncHandler(loginHandler)
        }

        // transaction handler
        router {
            httpMethods = listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
            paths = listOf("/product/buy")
            asyncHandler(transactionalHandler)
        }

        // error handler
        router(DEFAULT_LAST_ROUTER_ID + 1) {
            path = "*"
            asyncHandler(errorHandler)
        }
    }

    override fun order(): Int = 0
}