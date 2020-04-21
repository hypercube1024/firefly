package com.fireflysource.net.http.server.impl.router.handler

import com.fireflysource.common.string.Pattern
import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.model.HttpHeader.*
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.common.model.MimeTypes
import com.fireflysource.net.http.server.Router
import com.fireflysource.net.http.server.RoutingContext
import com.fireflysource.net.http.server.impl.content.provider.DefaultContentProvider
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer


class CorsHandler(val config: CorsConfig) : Router.Handler {

    companion object {
        private val simpleRequestMethods = setOf<String>(
            HttpMethod.GET.value,
            HttpMethod.HEAD.value,
            HttpMethod.POST.value
        )
        private val simpleRequestContentTypes = setOf<String>(
            MimeTypes.Type.FORM_ENCODED.value,
            MimeTypes.Type.MULTIPART_FORM_DATA.value,
            MimeTypes.Type.TEXT_PLAIN.value
        )
    }

    private val allowOriginPattern = Pattern.compile(config.allowOriginPattern, "*")

    override fun apply(ctx: RoutingContext): CompletableFuture<Void> {
        when {
            isSimpleRequest(ctx) -> handleSimpleRequest(ctx)
            isPreflightRequest(ctx) -> handlePreflightRequest(ctx)
            else -> {

            }
        }
        return if (ctx.response.isCommitted) Result.DONE else ctx.next()
    }

    private fun allowOrigin(ctx: RoutingContext): Boolean {
        val origin: String? = ctx.httpFields[ORIGIN]
        return !origin.isNullOrBlank() && allowOriginPattern.match(origin) != null
    }

    private fun isSimpleRequest(ctx: RoutingContext): Boolean {
        return if (ctx.httpFields.contains(ORIGIN) && simpleRequestMethods.contains(ctx.method)) {
            if (ctx.method == HttpMethod.POST.value)
                simpleRequestContentTypes.any { ctx.contentType.contains(it) }
            else true
        } else false
    }

    private fun isPreflightRequest(ctx: RoutingContext): Boolean {
        return ctx.httpFields.contains(ORIGIN)
                && ctx.method == HttpMethod.OPTIONS.value
                && ctx.httpFields.contains(ACCESS_CONTROL_REQUEST_METHOD)
    }

    private fun handleSimpleRequest(ctx: RoutingContext) {
        if (allowOrigin(ctx)) {
            val origin: String? = ctx.httpFields[ORIGIN]
            requireNotNull(origin)

            ctx.put(ACCESS_CONTROL_ALLOW_ORIGIN, origin)
                .put(ACCESS_CONTROL_ALLOW_CREDENTIALS, config.allowCredentials.toString())
            if (config.exposeHeaders.isNotEmpty()) {
                ctx.addCSV(ACCESS_CONTROL_EXPOSE_HEADERS, *config.exposeHeaders.toTypedArray())
            }
        } else handleNotAllowOrigin(ctx)
    }

    private fun handlePreflightRequest(ctx: RoutingContext) {
        if (!allowOrigin(ctx)) {
            handleNotAllowOrigin(ctx)
            return
        }

        if (!allowMethod(ctx)) {
            handleNotAllowMethod(ctx)
            return
        }

        if (!allowHeaders(ctx)) {
            handleNotAllowHeader(ctx)
            return
        }

        val origin: String? = ctx.httpFields[ORIGIN]
        requireNotNull(origin)

        ctx.setStatus(HttpStatus.NO_CONTENT_204)
            .put(ACCESS_CONTROL_ALLOW_ORIGIN, origin)
            .put(ACCESS_CONTROL_ALLOW_CREDENTIALS, config.allowCredentials.toString())
            .put(ACCESS_CONTROL_MAX_AGE, config.preflightMaxAge.toString())
            .addCSV(ACCESS_CONTROL_REQUEST_METHOD, *config.allowMethods.toTypedArray())
            .addCSV(ACCESS_CONTROL_REQUEST_HEADERS, *config.allowHeaders.toTypedArray())
            .end()
    }

    private fun allowMethod(ctx: RoutingContext): Boolean {
        val accessControlRequestMethods = ctx.httpFields.getCSV(ACCESS_CONTROL_REQUEST_METHOD, false)
        return config.allowMethods.containsAll(accessControlRequestMethods)
    }

    private fun allowHeaders(ctx: RoutingContext): Boolean {
        val accessControlRequestHeaders = ctx.httpFields.getCSV(ACCESS_CONTROL_REQUEST_HEADERS, false)
        return config.allowHeaders.containsAll(accessControlRequestHeaders)
    }


    private fun handleNotAllowOrigin(ctx: RoutingContext) {
        config.handleNotAllowOrigin.accept(ctx)
    }

    private fun handleNotAllowMethod(ctx: RoutingContext) {
        config.handleNotAllowMethod.accept(ctx)
    }

    private fun handleNotAllowHeader(ctx: RoutingContext) {
        config.handleNotAllowHeader.accept(ctx)
    }
}

data class CorsConfig(
    val allowOriginPattern: String,
    val exposeHeaders: Set<String>,
    val allowHeaders: Set<String>,
    val preflightMaxAge: Int = 86400,
    val allowCredentials: Boolean = true,
    val allowMethods: Set<String> = setOf(
        HttpMethod.GET.value,
        HttpMethod.POST.value,
        HttpMethod.PUT.value,
        HttpMethod.DELETE.value,
        HttpMethod.OPTIONS.value,
        HttpMethod.HEAD.value,
        HttpMethod.PATCH.value
    ),
    val handleNotAllowOrigin: Consumer<RoutingContext> = Consumer { ctx ->
        val origin: String? = ctx.httpFields[ORIGIN]
        ctx.contentProvider(
            DefaultContentProvider(
                HttpStatus.FORBIDDEN_403,
                NotAllowOriginException("Not allow origin: $origin"),
                ctx
            )
        ).end()
    },
    val handleNotAllowMethod: Consumer<RoutingContext> = Consumer { ctx ->
        val accessControlRequestMethods = ctx.httpFields.getCSV(ACCESS_CONTROL_REQUEST_METHOD, false)
        ctx.contentProvider(
            DefaultContentProvider(
                HttpStatus.METHOD_NOT_ALLOWED_405,
                NotAllowMethodException("Not allow methods: $accessControlRequestMethods"),
                ctx
            )
        ).end()
    },
    val handleNotAllowHeader: Consumer<RoutingContext> = Consumer { ctx ->
        val accessControlRequestHeaders = ctx.httpFields.getCSV(ACCESS_CONTROL_REQUEST_HEADERS, false)
        ctx.contentProvider(
            DefaultContentProvider(
                HttpStatus.BAD_REQUEST_400,
                NotAllowHeaderException("Not allow headers: $accessControlRequestHeaders"),
                ctx
            )
        ).end()
    }
)

class NotAllowOriginException(message: String) : IllegalArgumentException(message)

class NotAllowMethodException(message: String) : IllegalArgumentException(message)

class NotAllowHeaderException(message: String) : IllegalArgumentException(message)