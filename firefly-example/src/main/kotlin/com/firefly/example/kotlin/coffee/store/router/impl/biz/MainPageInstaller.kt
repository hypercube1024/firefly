package com.firefly.example.kotlin.coffee.store.router.impl.biz

import com.firefly.`$`
import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.codec.http2.model.HttpHeader
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.codec.http2.model.MimeTypes
import com.firefly.example.kotlin.coffee.store.ProjectConfig
import com.firefly.example.kotlin.coffee.store.router.RouterInstaller
import com.firefly.example.kotlin.coffee.store.service.OrderService
import com.firefly.example.kotlin.coffee.store.service.ProductService
import com.firefly.example.kotlin.coffee.store.vo.*
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.getAttr
import com.firefly.kotlin.ext.http.getJsonBody
import com.firefly.kotlin.ext.http.header
import com.firefly.server.http2.router.RoutingContext

/**
 * @author Pengtao Qiu
 */
@Component("mainPageInstaller")
class MainPageInstaller : RouterInstaller {

    @Inject
    private lateinit var config: ProjectConfig

    @Inject
    private lateinit var server: HttpServer

    @Inject
    private lateinit var productService: ProductService

    @Inject
    private lateinit var orderService: OrderService

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
                val query = toProductQuery(this)
                val page = productService.list(query)
                val userInfo = getAttr<UserInfo>(config.loginUserKey)
                val mainPage = MainPage(userInfo, page, query.type, query.searchKey)
                renderTemplate("${config.templateRoot}/index.mustache", mainPage)
            }
        }

        router {
            httpMethod = HttpMethod.POST
            path = "/product/buy"

            asyncCompleteHandler {
                val userInfo = getAttr<UserInfo>(config.loginUserKey) ?: throw IllegalStateException("The user does not login")
                val request = getJsonBody<ProductBuyRequest>()
                request.userId = userInfo.id
                orderService.buy(request)
                writeJson(Response(ResponseStatus.OK.value, ResponseStatus.OK.description, true))
            }
        }

    }

    private fun toProductQuery(ctx: RoutingContext): ProductQuery = ProductQuery(
            ctx.getParameter("searchKey"),
            ProductStatus.ENABLE.value,
            ctx.getParamOpt("type").filter { `$`.string.hasText(it) }.map<Int> { Integer.parseInt(it) }.orElse(0),
            ctx.getParamOpt("pageNumber").filter { `$`.string.hasText(it) }.map<Int> { Integer.parseInt(it) }.orElse(1),
            ctx.getParamOpt("pageSize").filter { `$`.string.hasText(it) }.map<Int> { Integer.parseInt(it) }.orElse(5)
    )

}