package com.firefly.example.kotlin.coffee.store.router.impl.biz

import com.firefly.`$`
import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.codec.http2.model.HttpStatus
import com.firefly.db.RecordNotFound
import com.firefly.example.kotlin.coffee.store.ProjectConfig
import com.firefly.example.kotlin.coffee.store.router.RouterInstaller
import com.firefly.example.kotlin.coffee.store.router.impl.sys.ErrorHandler
import com.firefly.example.kotlin.coffee.store.service.UserService
import com.firefly.example.kotlin.coffee.store.vo.UserInfo
import com.firefly.kotlin.ext.http.HttpServer
import com.firefly.kotlin.ext.log.KtLogger
import com.firefly.server.http2.router.RoutingContext
import com.firefly.utils.Assert
import kotlinx.coroutines.experimental.future.await
import java.util.*

/**
 * @author Pengtao Qiu
 */
@Component("userRouter")
class UserRouter : RouterInstaller {

    private val log = KtLogger.getLogger { }

    @Inject
    private lateinit var config: ProjectConfig

    @Inject
    private lateinit var server: HttpServer

    @Inject
    private lateinit var userService: UserService

    @Inject
    private lateinit var errorHandler: ErrorHandler

    override fun install() = server.addRouters {
        router {
            httpMethod = HttpMethod.GET
            path = config.logoutURL

            asyncCompleteHandler { logout(this) }
        }

        router {
            httpMethods = listOf(HttpMethod.GET, HttpMethod.POST)
            path = config.loginURL

            asyncCompleteHandler { login(this) }
        }
    }

    private suspend fun logout(ctx: RoutingContext) {
        ctx.removeSession().await()
        ctx.removeAttribute(config.loginUserKey)
        ctx.redirect(getBackURL(ctx))
        log.info("logout success!")
    }

    private suspend fun login(ctx: RoutingContext) {
        when (ctx.method) {
            HttpMethod.GET.asString() -> renderLoginPage(ctx)
            HttpMethod.POST.asString() -> verifyPasswordRequest(ctx)
            else -> errorHandler.renderError(ctx, HttpStatus.METHOD_NOT_ALLOWED_405)
        }
    }

    private suspend fun renderLoginPage(ctx: RoutingContext) {
        val backURL = ctx.getParameter("backURL")
        val map = HashMap<String, String>()
        map.put("backURL", backURL)
        ctx.renderTemplate("${config.templateRoot}/login.mustache", map)
    }

    private suspend fun verifyPasswordRequest(ctx: RoutingContext) {
        val username = ctx.getParameter("username")
        val password = ctx.getParameter("password")

        Assert.hasText(username, "The username is required")
        Assert.hasText(password, "The password is required")

        try {
            val user = userService.getByName(username)
            Assert.isTrue(user.password == password, "The password is incorrect")

            val session = ctx.session.await()
            val userInfo = UserInfo(user.id ?: 0, user.name)
            session.attributes[config.loginUserKey] = userInfo
            session.maxInactiveInterval = config.sessionMaxInactiveInterval
            ctx.updateSession(session).await()
            ctx.setAttribute(config.loginUserKey, userInfo)
            ctx.redirect(getBackURL(ctx))
            log.info("user $userInfo login success!")
        } catch (e: RecordNotFound) {
            throw IllegalArgumentException("The username is incorrect")
        }
    }

    private fun getBackURL(ctx: RoutingContext) =
        ctx.getParamOpt("backURL").filter { `$`.string.hasText(it) }.orElse("/")
}