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
import kotlinx.coroutines.experimental.NonCancellable
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run
import java.io.Closeable
import java.net.InetAddress
import java.util.function.Supplier
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Firefly HTTP server extensions
 *
 * @author Pengtao Qiu
 */

// HTTP server API extensions

fun HTTP2ServerBuilder.asyncHandler(handler: suspend RoutingContext.(context: CoroutineContext) -> Unit): HTTP2ServerBuilder = this.handler {
    it.response.isAsynchronous = true
    launch(AsyncPool) {
        handler.invoke(it, context)
    }
}

fun Router.asyncHandler(handler: suspend RoutingContext.(context: CoroutineContext) -> Unit): Router = this.handler {
    it.response.isAsynchronous = true
    launch(AsyncPool) {
        handler.invoke(it, context)
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
inline fun RoutingContext.statusLine(block: StatusLineBlock.() -> Unit): Unit = block.invoke(StatusLineBlock(this))

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

    override fun toString(): String {
        return "StatusLineBlock(status=$status, reason='$reason', httpVersion=$httpVersion)"
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
inline fun RoutingContext.header(block: HeaderBlock.() -> Unit): Unit = block.invoke(HeaderBlock(this))

class HeaderBlock(ctx: RoutingContext) : HttpFieldOperator {

    val httpFields: HttpFields = ctx.response.fields

    override infix fun String.to(value: String): Unit {
        httpFields.put(this, value)
    }

    override infix fun HttpHeader.to(value: String): Unit {
        httpFields.put(this, value)
    }

    override operator fun HttpField.unaryPlus(): Unit {
        httpFields.add(this)
    }

    override fun toString(): String {
        return "HeaderBlock(httpFields=$httpFields)"
    }
}

/**
 * Response HTTP trailer block
 *
 * @param block HTTP trailer statement
 */
inline fun RoutingContext.trailer(block: TrailerBlock.() -> Unit): Unit = block.invoke(TrailerBlock(this))

class TrailerBlock(ctx: RoutingContext) : Supplier<HttpFields>, HttpFieldOperator {

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

    override fun toString(): String {
        return "TrailerBlock(httpFields=$httpFields)"
    }

}

class RouterBlock(private val router: Router) {

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

    fun asyncHandler(handler: suspend RoutingContext.(context: CoroutineContext) -> Unit): Unit {
        router.asyncHandler(handler)
    }

    fun handler(handler: RoutingContext.() -> Unit): Unit {
        router.handler(handler)
    }

    suspend fun <T : Closeable?, R> T.safeUse(block: (T) -> R): R {
        var closed = false
        try {
            return block(this)
        } catch (e: Exception) {
            try {
                run(NonCancellable) {
                    closed = true
                    this?.close()
                }
            } catch (closeException: Exception) {
            }
            throw e
        } finally {
            if (!closed) {
                run(NonCancellable) {
                    this?.close()
                }
            }
        }
    }

    override fun toString(): String {
        return "RouterBlock(method='$method', methods=$methods, httpMethod=$httpMethod, httpMethods=$httpMethods, path='$path', regexPath='$regexPath', paths=$paths, consumes='$consumes', produces='$produces')"
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

    inline fun router(block: RouterBlock.() -> Unit): Unit {
        block.invoke(RouterBlock(routerManager.register()))
    }

    inline fun addRouters(block: HttpServer.() -> Unit): Unit = block.invoke(this)
}