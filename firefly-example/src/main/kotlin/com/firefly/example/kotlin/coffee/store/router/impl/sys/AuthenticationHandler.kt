package com.firefly.example.kotlin.coffee.store.router.impl.sys

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.codec.http2.encode.UrlEncoded
import com.firefly.example.kotlin.coffee.store.ProjectConfig
import com.firefly.kotlin.ext.http.AsyncHandler
import com.firefly.kotlin.ext.http.asyncSucceed
import com.firefly.kotlin.ext.log.KtLogger
import com.firefly.server.http2.router.RoutingContext
import kotlinx.coroutines.future.await
import java.nio.charset.StandardCharsets

/**
 * @author Pengtao Qiu
 */
@Component("authenticationHandler")
class AuthenticationHandler : AsyncHandler {

    private val log = KtLogger.getLogger { }

    @Inject
    private lateinit var projectConfig: ProjectConfig

    override suspend fun handle(ctx: RoutingContext) {
        val session = ctx.getSession(false).await()
        val userInfo = session.attributes[projectConfig.loginUserKey]
        if (userInfo != null) {
            ctx.setAttribute(projectConfig.loginUserKey, userInfo)
            ctx.next()
        } else {
            val urlEncoded = UrlEncoded()
            urlEncoded.put("backURL", ctx.uri.pathQuery)
            ctx.redirect("${projectConfig.loginURL}?${urlEncoded.encode(StandardCharsets.UTF_8, true)}")
            ctx.asyncSucceed(Unit)
        }
    }

}