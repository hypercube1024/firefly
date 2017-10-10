package com.firefly.example.kotlin.coffee.store.router.impl.sys

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.codec.http2.model.HttpStatus
import com.firefly.example.kotlin.coffee.store.router.RouterInstaller
import com.firefly.example.kotlin.coffee.store.vo.Response
import com.firefly.example.kotlin.coffee.store.vo.ResponseStatus
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.asyncNext
import com.firefly.kotlin.ext.log.Log
import com.firefly.server.http2.router.handler.session.LocalHTTPSessionHandler
import java.lang.IllegalArgumentException

/**
 * @author Pengtao Qiu
 */
@Component("sysRouterInstaller")
class SysRouterInstaller : RouterInstaller {

    companion object {
        private val log = Log.getLogger { }
    }

    @Inject
    private lateinit var server: HttpServer

    @Inject
    private lateinit var localHTTPSessionHandler: LocalHTTPSessionHandler

    @Inject
    private lateinit var staticResourceHandler: StaticResourceHandler

    override fun install() = server.addRouters {
        router {
            path = "*"

            asyncHandler {
                asyncNext<Unit>({
                    log.info("request end -> $uri")
                    end()
                }, {
                    when (it) {
                        is IllegalArgumentException -> {
                            setStatus(HttpStatus.OK_200)
                            writeJson(Response(ResponseStatus.ARGUMENT_ERROR.value,
                                    it.message ?: ResponseStatus.ARGUMENT_ERROR.description,
                                    false)).end()
                        }
                        is IllegalStateException -> {
                            setStatus(HttpStatus.OK_200)
                            writeJson(Response(ResponseStatus.SERVER_ERROR.value,
                                    it.message ?: ResponseStatus.SERVER_ERROR.description,
                                    false)).end()
                        }
                        else -> {
                            log.error("server exception", it)
                            setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500)
                            writeJson(Response(ResponseStatus.SERVER_ERROR.value,
                                    it?.message ?: ResponseStatus.SERVER_ERROR.description,
                                    false)).end()
                        }
                    }
                })
            }
        }

        // session handler
        router {
            path = "*"
            handler(localHTTPSessionHandler)
        }

        // static file handler
        router {
            httpMethod = HttpMethod.GET
            paths = staticResourceHandler.staticResources
            handler(staticResourceHandler)
        }
    }

    override fun order(): Int = 0
}