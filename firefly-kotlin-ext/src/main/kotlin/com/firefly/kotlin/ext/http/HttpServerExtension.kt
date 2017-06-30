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
import java.util.function.Supplier

/**
 * Firefly HTTP server extensions
 *
 * @author Pengtao Qiu
 */

// HTTP server API extensions

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


// HTTP server DSL

/**
 * Response status line block
 *
 * @param block Response status line statement
 */
fun RoutingContext.statusLine(block: StatusLineBlock.() -> Unit): Unit = block.invoke(StatusLineBlock(this))

class StatusLineBlock(private val ctx: RoutingContext) {
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

interface HttpFieldOperator {
    infix fun String.to(value: String): Unit

    infix fun HttpHeader.to(value: String): Unit

    operator fun HttpField.unaryPlus(): Unit
}

/**
 * Response HTTP header block
 *
 * @param block HTTP header statement
 */
fun RoutingContext.header(block: HeaderBlock.() -> Unit): Unit = block.invoke(HeaderBlock(this))

class HeaderBlock(private val ctx: RoutingContext) : HttpFieldOperator {

    override infix fun String.to(value: String): Unit {
        ctx.put(this, value)
    }

    override infix fun HttpHeader.to(value: String): Unit {
        ctx.put(this, value)
    }

    override operator fun HttpField.unaryPlus(): Unit {
        ctx.response.fields.add(this)
    }

}

fun RoutingContext.trailer(block: TrailerBlock.() -> Unit): Unit = block.invoke(TrailerBlock(this))

class TrailerBlock(private val ctx: RoutingContext) : Supplier<HttpFields>, HttpFieldOperator {

    val httpFields: HttpFields = HttpFields()

    init {
        ctx.response.trailerSupplier = this
    }

    override fun get(): HttpFields = httpFields

    override infix fun String.to(value: String): Unit {
        httpFields.put(this, value)
    }

    override infix fun HttpHeader.to(value: String): Unit {
        httpFields.put(this, value)
    }

    override operator fun HttpField.unaryPlus(): Unit {
        httpFields.add(this)
    }
}

class RouterWrap(private val router: Router) {

    var method: String = HttpMethod.GET.asString()
        set(value) {
            router.method(value)
            field = value
        }

    var methods: List<String> = listOf(HttpMethod.GET.asString(), HttpMethod.POST.asString())
        set(value) {
            value.forEach { router.method(it) }
            field = value
        }

    var httpMethod: HttpMethod = HttpMethod.GET
        set(value) {
            router.method(value)
            field = value
        }

    var httpMethods: List<HttpMethod> = listOf(HttpMethod.GET, HttpMethod.POST)
        set(value) {
            httpMethods.forEach { router.method(it) }
            field = value
        }

    var path: String = ""
        set(value) {
            router.path(value)
            field = value
        }

    var regexPath: String = ""
        set(value) {
            router.pathRegex(value)
            field = value
        }

    var paths: List<String> = listOf()
        set(value) {
            value.forEach { router.path(it) }
            field = value
        }

    var consumes: String = ""
        set(value) {
            router.consumes(value)
            field = value
        }

    var produces: String = ""
        set(value) {
            router.produces(value)
            field = value
        }

    fun asyncHandler(handler: suspend RoutingContext.() -> Unit): Unit {
        router.asyncHandler(handler)
    }

    fun handler(handler: RoutingContext.() -> Unit): Unit {
        router.handler(handler)
    }
}

interface HttpServerLifecycle {
    fun stop(): Unit

    fun listen(host: String, port: Int): Unit

    fun listen(port: Int): Unit

    fun listen(): Unit
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