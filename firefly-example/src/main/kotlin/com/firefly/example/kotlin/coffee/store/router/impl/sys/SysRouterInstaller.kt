package com.firefly.example.kotlin.coffee.store.router.impl.sys

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.example.kotlin.coffee.store.router.RouterInstaller
import com.firefly.kotlin.ext.http.HttpServer
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
    private lateinit var staticResourceHandler: StaticResourceHandler

    @Inject
    private lateinit var loginHandler: LoginHandler

    @Inject
    private lateinit var transactionalHandler: TransactionalHandler

    override fun install() = server.addRouters {
        // global handler
        router {
            path = "*"
            asyncHandler(globalHandler)
        }

        // session handler
        router {
            path = "*"
            asyncHandler { localHTTPSessionHandler.handle(this) }
        }

        // login handler
        router {
            path = "*"
            asyncHandler(loginHandler)
        }

        // static file handler
        router {
            httpMethod = HttpMethod.GET
            paths = staticResourceHandler.staticResources
            asyncHandler(staticResourceHandler)
        }

        // transaction handler
        router {
            httpMethods = listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)
            paths = listOf("/product/buy")
            asyncHandler(transactionalHandler)
        }

    }

    override fun order(): Int = 0
}