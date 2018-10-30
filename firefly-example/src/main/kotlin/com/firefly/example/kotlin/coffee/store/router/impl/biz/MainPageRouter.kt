package com.firefly.example.kotlin.coffee.store.router.impl.biz

import com.firefly.`$`
import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.codec.http2.model.HttpHeader
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.codec.http2.model.MimeTypes
import com.firefly.example.kotlin.coffee.store.ProjectConfig
import com.firefly.example.kotlin.coffee.store.router.RouterInstaller
import com.firefly.example.kotlin.coffee.store.service.ProductService
import com.firefly.example.kotlin.coffee.store.vo.MainPage
import com.firefly.example.kotlin.coffee.store.vo.ProductQuery
import com.firefly.example.kotlin.coffee.store.vo.ProductStatus
import com.firefly.example.kotlin.coffee.store.vo.UserInfo
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.header
import com.firefly.server.http2.router.RoutingContext
import com.firefly.server.http2.router.SessionNotFound
import kotlinx.coroutines.future.await

/**
 * @author Pengtao Qiu
 */
@Component("mainPageRouter")
class MainPageRouter : RouterInstaller {

    @Inject
    private lateinit var config: ProjectConfig

    @Inject
    private lateinit var server: HttpServer

    @Inject
    private lateinit var productService: ProductService

    override fun install() = server.addRouters {
        router {
            httpMethod = HttpMethod.GET
            path = "/hello"

            asyncCompleteHandler { write("hello coffee store") }
        }

        router {
            httpMethod = HttpMethod.GET
            path = "/"

            asyncCompleteHandler {
                header {
                    HttpHeader.CONTENT_TYPE to MimeTypes.Type.TEXT_HTML.asString()
                }

                val userInfo: UserInfo? = try {
                    val session = getSession(false).await()
                    val u = session.attributes[config.loginUserKey]
                    if (u != null) {
                        setAttribute(config.loginUserKey, u)
                        u as UserInfo
                    } else {
                        null
                    }
                } catch (e: SessionNotFound) {
                    null
                }

                val query = toProductQuery(this)
                val page = productService.list(query)
                val mainPage = MainPage(userInfo, page, query.type, query.searchKey)
                renderTemplate("${config.templateRoot}/index.mustache", mainPage)
            }
        }
    }

    private fun toProductQuery(ctx: RoutingContext): ProductQuery = ProductQuery(
        ctx.getParameter("searchKey"),
        ProductStatus.ENABLE.value,
        ctx.getParamOpt("type").filter { `$`.string.hasText(it) }.map { Integer.parseInt(it) }.orElse(0),
        ctx.getParamOpt("pageNumber").filter { `$`.string.hasText(it) }.map { Integer.parseInt(it) }.orElse(1),
        ctx.getParamOpt("pageSize").filter { `$`.string.hasText(it) }.map { Integer.parseInt(it) }.orElse(5)
                                                                                )

}