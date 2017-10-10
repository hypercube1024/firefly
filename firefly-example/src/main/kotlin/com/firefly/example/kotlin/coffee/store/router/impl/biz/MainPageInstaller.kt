package com.firefly.example.kotlin.coffee.store.router.impl.biz

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.example.kotlin.coffee.store.router.RouterInstaller
import com.firefly.kotlin.ext.http.HttpServer

/**
 * @author Pengtao Qiu
 */
@Component("mainPageInstaller")
class MainPageInstaller : RouterInstaller {

    @Inject
    private lateinit var server: HttpServer

    override fun install() = server.addRouters {
        router {
            httpMethod = HttpMethod.GET
            path = "/hello"

            asyncCompleteHandler { write("hello coffee store") }
        }
    }

}