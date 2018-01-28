package com.firefly.example.kotlin.coffee.store.router.impl.biz

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.example.kotlin.coffee.store.ProjectConfig
import com.firefly.example.kotlin.coffee.store.router.RouterInstaller
import com.firefly.example.kotlin.coffee.store.service.OrderService
import com.firefly.example.kotlin.coffee.store.service.ProductService
import com.firefly.example.kotlin.coffee.store.vo.ProductBuyRequest
import com.firefly.example.kotlin.coffee.store.vo.Response
import com.firefly.example.kotlin.coffee.store.vo.ResponseStatus
import com.firefly.example.kotlin.coffee.store.vo.UserInfo
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.http.getAttr
import com.firefly.kotlin.ext.http.getJsonBody
import com.firefly.server.http2.router.RoutingContext

/**
 * @author Pengtao Qiu
 */
@Component("productRouter")
class ProductRouter : RouterInstaller {

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
            httpMethod = HttpMethod.POST
            path = "/product/buy"

            asyncCompleteHandler {
                val request = toProductBuyRequest(this)
                orderService.buy(request)
                writeJson(Response(ResponseStatus.OK.value, ResponseStatus.OK.description, true))
            }
        }
    }

    private fun toProductBuyRequest(ctx: RoutingContext): ProductBuyRequest {
        val userInfo = ctx.getAttr<UserInfo>(config.loginUserKey)
                ?: throw IllegalStateException("The user does not login")
        val request = ctx.getJsonBody<ProductBuyRequest>()
        request.userId = userInfo.id
        return request
    }
}