package com.firefly.example.kotlin.coffee.store.router.impl.sys

import com.firefly.`$`
import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.codec.http2.encode.UrlEncoded
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.codec.http2.model.HttpStatus
import com.firefly.db.RecordNotFound
import com.firefly.example.kotlin.coffee.store.ProjectConfig
import com.firefly.example.kotlin.coffee.store.service.UserService
import com.firefly.example.kotlin.coffee.store.vo.UserInfo
import com.firefly.kotlin.ext.http.AsyncHandler
import com.firefly.kotlin.ext.http.asyncFail
import com.firefly.kotlin.ext.http.asyncSucceed
import com.firefly.kotlin.ext.log.Log
import com.firefly.server.http2.router.HTTPSession
import com.firefly.server.http2.router.RoutingContext
import com.firefly.utils.pattern.Pattern
import kotlinx.coroutines.experimental.future.await
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * @author Pengtao Qiu
 */
@Component("loginHandler")
class LoginHandler : AsyncHandler {

    private val log = Log.getLogger { }

    private val uriWhitelist: List<Pattern> = Stream.of(
            "/",
            "/hello",
            "/favicon.ico",
            "/static/*",
            "/products")
            .map { p -> Pattern.compile(p, "*") }
            .collect(Collectors.toList())

    @Inject
    private lateinit var projectConfig: ProjectConfig

    @Inject
    private lateinit var errorHandler: ErrorHandler

    @Inject
    private lateinit var userService: UserService

    override suspend fun handle(ctx: RoutingContext) {
        try {
            val session = ctx.session.await()
            when (ctx.uri.path) {
                projectConfig.loginURL -> login(ctx, session)
                projectConfig.logoutURL -> logout(ctx)
                else -> verifyLogin(ctx, session)
            }
        } catch (e: Exception) {
            ctx.asyncFail<Unit>(e)
        }
    }

    private suspend fun login(ctx: RoutingContext, session: HTTPSession) {
        when (ctx.method) {
            HttpMethod.GET.asString() -> renderLoginPage(ctx)
            HttpMethod.POST.asString() -> verifyPasswordRequest(ctx, session)
            else -> errorHandler.renderError(ctx, HttpStatus.METHOD_NOT_ALLOWED_405)
        }
    }

    private suspend fun renderLoginPage(ctx: RoutingContext) {
        val backURL = ctx.getParameter("backURL")
        val map = HashMap<String, String>()
        map.put("backURL", backURL)
        ctx.renderTemplate("${projectConfig.templateRoot}/login.mustache", map)
        ctx.asyncSucceed(Unit)
    }

    private suspend fun verifyPasswordRequest(ctx: RoutingContext, session: HTTPSession) {
        val username = ctx.getParameter("username")
        val password = ctx.getParameter("password")

        if (!`$`.string.hasText(username)) {
            ctx.asyncFail<Unit>(IllegalArgumentException("The username is required"))
            return
        }

        if (!`$`.string.hasText(password)) {
            ctx.asyncFail<Unit>(IllegalArgumentException("The password is required"))
            return
        }

        try {
            val user = userService.getByName(username)
            if (user.password != password) {
                ctx.asyncFail<Unit>(IllegalArgumentException("The password is incorrect"))
            } else {
                val userInfo = UserInfo(user.id ?: 0, user.name)
                session.attributes.put(projectConfig.loginUserKey, userInfo)
                session.maxInactiveInterval = projectConfig.sessionMaxInactiveInterval
                ctx.updateSession(session).await()
                ctx.setAttribute(projectConfig.loginUserKey, userInfo)
                ctx.redirect(getBackURL(ctx))
                log.info("user $userInfo login success!")
                ctx.asyncSucceed(Unit)
            }
        } catch (e: RecordNotFound) {
            ctx.asyncFail<Unit>(IllegalArgumentException("The username is incorrect"))
        }
    }

    private suspend fun logout(ctx: RoutingContext) {
        ctx.removeSession().await()
        ctx.removeAttribute(projectConfig.loginUserKey)
        ctx.redirect(getBackURL(ctx))
        log.info("logout success!")
        ctx.asyncSucceed(Unit)
    }

    private suspend fun verifyLogin(ctx: RoutingContext, session: HTTPSession) {
        val userInfo = session.attributes[projectConfig.loginUserKey]

        if (userInfo != null) {
            ctx.setAttribute(projectConfig.loginUserKey, userInfo)
            ctx.next()
        } else {
            if (skipVerify(ctx.uri.path)) {
                ctx.next()
            } else {
                val urlEncoded = UrlEncoded()
                urlEncoded.put("backURL", ctx.uri.pathQuery)
                ctx.redirect( "${projectConfig.loginURL}?${urlEncoded.encode(StandardCharsets.UTF_8, true)}")
                ctx.asyncSucceed(Unit)
            }
        }
    }

    private fun skipVerify(uri: String): Boolean {
        return uriWhitelist.parallelStream().anyMatch { p -> p.match(uri) != null }
    }

    private fun getBackURL(ctx: RoutingContext) = ctx.getParamOpt("backURL").filter { `$`.string.hasText(it) }.orElse("/")

}