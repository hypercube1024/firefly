package com.firefly.kotlin.ext.http

import com.firefly.client.http2.SimpleHTTPClient
import com.firefly.client.http2.SimpleResponse
import com.firefly.codec.http2.model.HttpHeader
import com.firefly.codec.http2.model.HttpMethod
import com.firefly.codec.http2.model.MimeTypes
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
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.launch
import java.net.InetAddress

/**
 * @author Pengtao Qiu
 */

// HTTP server extension

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

interface HttpServerLifecycle {
    fun stop(): Unit

    fun listen(host: String, port: Int): Unit

    fun listen(port: Int): Unit

    fun listen(): Unit
}

class RouterWrap(private val router: Router,
                 var method: String = HttpMethod.GET.asString(),
                 var path: String? = null,
                 var regexPath: String? = null,
                 var paths: List<String>? = null,
                 var consumes: String? = null,
                 var produces: String? = null) {

    private var isSetup: Boolean = false

    var httpMethod: HttpMethod = HttpMethod.GET
        set(value) {
            method = value.asString()
        }

    fun asyncHandler(handler: suspend RoutingContext.() -> Unit): Unit {
        setup()
        router.asyncHandler(handler)
    }

    fun handler(handler: RoutingContext.() -> Unit): Unit {
        setup()
        router.handler(handler)
    }

    private fun setup() {
        if (!isSetup) {
            router.method(method)

            if (path != null) {
                router.path(path)
            }

            if (regexPath != null) {
                router.pathRegex(regexPath)
            }

            paths?.forEach {
                router.path(it)
            }

            if (consumes != null) {
                router.consumes(consumes)
            }

            if (produces != null) {
                router.produces(produces)
            }
            isSetup = true
        }
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

// HTTP client extension

inline fun <reified T : Any> SimpleResponse.getJsonBody(charset: String): T = Json.parse(getStringBody(charset))

inline fun <reified T : Any> SimpleResponse.getJsonBody(): T = Json.parse(stringBody)

suspend fun SimpleHTTPClient.RequestBuilder.asyncSubmit(): SimpleResponse = submit().await()