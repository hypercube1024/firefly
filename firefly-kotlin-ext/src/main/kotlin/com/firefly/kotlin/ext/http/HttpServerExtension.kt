package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.*
import com.firefly.kotlin.ext.common.AsyncPool
import com.firefly.kotlin.ext.common.Json
import com.firefly.server.http2.HTTP2ServerBuilder
import com.firefly.server.http2.SimpleHTTPServer
import com.firefly.server.http2.SimpleHTTPServerConfiguration
import com.firefly.server.http2.SimpleRequest
import com.firefly.server.http2.router.Router
import com.firefly.server.http2.router.RouterManager
import com.firefly.server.http2.router.RoutingContext
import com.firefly.server.http2.router.handler.body.HTTPBodyConfiguration
import kotlinx.coroutines.experimental.launch
import java.net.InetAddress

/**
 * Firefly HTTP server extensions
 *
 * @author Pengtao Qiu
 */

// HTTP server extensions

fun HTTP2ServerBuilder.asyncHandler(handler: suspend RoutingContext.() -> Unit): HTTP2ServerBuilder = this.handler {
    it.response.isAsynchronous = true
    launch(AsyncPool) {
        handler.invoke(it)
    }
}

fun Router.asyncHandler(handler: suspend RoutingContext.() -> Unit): Router = this.handler {
    it.response.isAsynchronous = true
    launch(AsyncPool) {
        handler.invoke(it)
    }
}

inline fun <reified T : Any> RoutingContext.getJsonBody(charset: String): T = Json.parse(getStringBody(charset))

inline fun <reified T : Any> RoutingContext.getJsonBody(): T = Json.parse(stringBody)

fun RoutingContext.writeJson(obj: Any): RoutingContext = put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.APPLICATION_JSON.asString()).write(Json.toJson(obj))

inline fun <reified T : Any> SimpleRequest.getJsonBody(charset: String): T = Json.parse(getStringBody(charset))

inline fun <reified T : Any> SimpleRequest.getJsonBody(): T = Json.parse(stringBody)

fun RoutingContext.header(block: HeaderWrap.() -> Unit): Unit = block.invoke(HeaderWrap(this))

fun RoutingContext.statusLine(block: StatusLineWrap.() -> Unit): Unit = block.invoke(StatusLineWrap(this))


// HTTP server DSL

interface HttpServerLifecycle {
    fun stop(): Unit

    fun listen(host: String, port: Int): Unit

    fun listen(port: Int): Unit

    fun listen(): Unit
}

class StatusLineWrap(private val ctx: RoutingContext) {
    var status: Int = HttpStatus.OK_200
        set(value) {
            ctx.setStatus(value)
            field = value
        }

    var reason: String = HttpStatus.Code.OK.message
        set(value) {
            ctx.setReason(value)
            field = value
        }

    var httpVersion: HttpVersion = HttpVersion.HTTP_1_1
        set(value) {
            ctx.setHttpVersion(value)
            field = value
        }
}

class HeaderWrap(private val ctx: RoutingContext) {

    infix fun String.to(value: String): Unit {
        ctx.put(this, value)
    }

    infix fun HttpHeader.to(value: String): Unit {
        ctx.put(this, value)
    }
}

class RouterWrap(private val router: Router) {

    var method: String = HttpMethod.GET.asString()
        set(value) {
            router.method(value)
            field = value
        }

    var httpMethod: HttpMethod = HttpMethod.GET
        set(value) {
            router.method(value)
            field = value
        }

    var path: String? = null
        set(value) {
            if (value != null) {
                router.path(value)
                field = value
            }
        }

    var regexPath: String? = null
        set(value) {
            if (value != null) {
                router.pathRegex(value)
                field = value
            }
        }

    var paths: List<String>? = null
        set(value) {
            value?.forEach {
                router.path(it)
            }
            field = value
        }

    var consumes: String? = null
        set(value) {
            if (value != null) {
                router.consumes(value)
                field = value
            }
        }

    var produces: String? = null
        set(value) {
            if (value != null) {
                router.produces(value)
                field = value
            }
        }

    init {
        router.method(method)
    }

    fun asyncHandler(handler: suspend RoutingContext.() -> Unit): Unit {
        router.asyncHandler(handler)
    }

    fun handler(handler: RoutingContext.() -> Unit): Unit {
        router.handler(handler)
    }
}

class HttpServer(serverConfiguration: SimpleHTTPServerConfiguration = SimpleHTTPServerConfiguration(),
                 httpBodyConfiguration: HTTPBodyConfiguration = HTTPBodyConfiguration(),
                 block: HttpServer.() -> Unit) : HttpServerLifecycle {

    val server: SimpleHTTPServer = SimpleHTTPServer(serverConfiguration)
    val routerManager: RouterManager = RouterManager.create(httpBodyConfiguration)

    init {
        block.invoke(this)
    }

    constructor(block: HttpServer.() -> Unit) : this(SimpleHTTPServerConfiguration(), HTTPBodyConfiguration(), block)

    constructor() : this({})

    override fun stop() = server.stop()

    override fun listen(host: String, port: Int) = server.headerComplete(routerManager::accept).listen(host, port)

    override fun listen(port: Int) = listen(InetAddress.getLocalHost().hostAddress, port)

    override fun listen() = server.headerComplete(routerManager::accept).listen()

    fun router(block: RouterWrap.() -> Unit): Unit {
        block.invoke(RouterWrap(routerManager.register()))
    }

    fun addRouters(block: HttpServer.() -> Unit): Unit = block.invoke(this)
}