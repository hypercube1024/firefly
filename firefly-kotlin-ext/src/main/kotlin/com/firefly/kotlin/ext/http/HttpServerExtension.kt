package com.firefly.kotlin.ext.http

import com.firefly.codec.http2.model.*
import com.firefly.codec.websocket.frame.Frame
import com.firefly.codec.websocket.stream.AbstractWebSocketBuilder
import com.firefly.codec.websocket.stream.WebSocketConnection
import com.firefly.kotlin.ext.common.CoroutineLocal
import com.firefly.kotlin.ext.common.Json
import com.firefly.kotlin.ext.log.KtLogger
import com.firefly.server.http2.SimpleHTTPServer
import com.firefly.server.http2.SimpleHTTPServerConfiguration
import com.firefly.server.http2.SimpleRequest
import com.firefly.server.http2.WebSocketHandler
import com.firefly.server.http2.router.Handler
import com.firefly.server.http2.router.Router
import com.firefly.server.http2.router.RouterManager
import com.firefly.server.http2.router.RoutingContext
import com.firefly.server.http2.router.handler.body.HTTPBodyConfiguration
import com.firefly.server.http2.router.handler.error.DefaultErrorResponseHandlerLoader
import com.firefly.server.http2.router.impl.RoutingContextImpl
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.future.await
import java.io.Closeable
import java.net.InetAddress
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.coroutineContext

/**
 * Firefly HTTP server extensions.
 *
 * @author Pengtao Qiu
 */
val sysLogger = KtLogger.getLogger("firefly-system")

// HTTP server API extensions
inline fun <reified T : Any> RoutingContext.getJsonBody(charset: String): T = Json.parse(getStringBody(charset))

inline fun <reified T : Any> RoutingContext.getJsonBody(): T = Json.parse(stringBody)

inline fun <reified T : Any> RoutingContext.getAttr(name: String): T? {
    val data = getAttribute(name) ?: return null
    if (data is T) {
        return data
    } else {
        throw ClassCastException("The attribute $name type is ${data::class.java}. It can't cast to ${T::class.java}")
    }
}

inline fun <reified T : Any> SimpleRequest.getJsonBody(charset: String): T = Json.parse(getStringBody(charset))

inline fun <reified T : Any> SimpleRequest.getJsonBody(): T = Json.parse(stringBody)

data class AsyncPromise<in C>(val succeeded: suspend (C) -> Unit, val failed: suspend (Throwable?) -> Unit)

val promiseQueueKey = "_promiseQueue"

fun <C> RoutingContext.getPromiseQueue(): Deque<AsyncPromise<C>>? = getAttr(promiseQueueKey)

@Suppress("UNCHECKED_CAST")
fun <C> RoutingContext.createPromiseQueueIfAbsent(): Deque<AsyncPromise<C>> = attributes.computeIfAbsent(promiseQueueKey) { ConcurrentLinkedDeque<AsyncPromise<C>>() } as Deque<AsyncPromise<C>>

/**
 * Set the callback that is called when the asynchronous handler finishes.
 */
fun <C> RoutingContext.asyncComplete(succeeded: suspend (C) -> Unit, failed: suspend (Throwable?) -> Unit): RoutingContext {
    val queue = createPromiseQueueIfAbsent<C>()
    queue.push(AsyncPromise(succeeded, failed))
    return this
}

fun <C> RoutingContext.asyncComplete(succeeded: suspend (C) -> Unit): RoutingContext {
    asyncComplete(succeeded, { this.asyncFail<Throwable?>(it) })
    return this
}

/**
 * Execute the next asynchronous handler and set the callback is called when the asynchronous handler finishes.
 */
fun <C> RoutingContext.asyncNext(succeeded: suspend (C) -> Unit, failed: suspend (Throwable?) -> Unit): Boolean {
    asyncComplete(succeeded, failed)
    return next()
}

fun <C> RoutingContext.asyncNext(succeeded: suspend (C) -> Unit): Boolean {
    asyncComplete(succeeded, { this.asyncFail<Throwable?>(it) })
    return next()
}

suspend fun <C> RoutingContext.asyncNext(): Pair<Boolean, C> {
    val future = CompletableFuture<C>()
    val hasNext = asyncNext<C>({ future.complete(it) }, { future.completeExceptionally(it) })
    return hasNext to future.await()
}

suspend fun <C> RoutingContext.asyncNext(time: Long, unit: TimeUnit): Pair<Boolean, C> {
    val future = CompletableFuture<C>()
    val hasNext = asyncNext<C>({ future.complete(it) }, { future.completeExceptionally(it) })
    return withTimeout(time, unit) { hasNext to future.await() }
}

/**
 * Execute asynchronous succeeded callback.
 */
suspend fun <C> RoutingContext.asyncSucceed(result: C) {
    getPromiseQueue<C>()?.pop()?.succeeded?.invoke(result)
}

/**
 * Execute asynchronous failed callback
 */
suspend fun <C> RoutingContext.asyncFail(x: Throwable? = null) {
    getPromiseQueue<C>()?.pop()?.failed?.invoke(x)
}


// HTTP server DSL

/**
 * Response status line block
 *
 * @param block Response status line statement
 */
inline fun RoutingContext.statusLine(block: StatusLineBlock.() -> Unit) = block.invoke(StatusLineBlock(this))

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
            ctx.httpVersion = value
            field = value
        }

    override fun toString(): String = "StatusLineBlock(status=$status, reason='$reason', httpVersion=$httpVersion)"

}

interface HttpFieldOperator {
    infix fun String.to(value: String)

    infix fun HttpHeader.to(value: String)

    operator fun HttpField.unaryPlus()
}

/**
 * Response HTTP header block
 *
 * @param block HTTP header statement
 */
inline fun RoutingContext.header(block: HeaderBlock.() -> Unit) = block.invoke(HeaderBlock(this))

class HeaderBlock(ctx: RoutingContext) : HttpFieldOperator {

    val httpFields: HttpFields = ctx.response.fields

    override infix fun String.to(value: String) {
        httpFields.put(this, value)
    }

    override infix fun HttpHeader.to(value: String) {
        httpFields.put(this, value)
    }

    override operator fun HttpField.unaryPlus() {
        httpFields.add(this)
    }

    override fun toString(): String = "HeaderBlock(httpFields=$httpFields)"
}

/**
 * Response HTTP trailer block
 *
 * @param block HTTP trailer statement
 */
inline fun RoutingContext.trailer(block: TrailerBlock.() -> Unit) = block.invoke(TrailerBlock(this))

class TrailerBlock(ctx: RoutingContext) : Supplier<HttpFields>, HttpFieldOperator {

    val httpFields: HttpFields = HttpFields()

    init {
        ctx.response.trailerSupplier = this
    }

    override fun get(): HttpFields = httpFields

    override infix fun String.to(value: String) {
        httpFields.put(this, value)
    }

    override infix fun HttpHeader.to(value: String) {
        httpFields.put(this, value)
    }

    override operator fun HttpField.unaryPlus() {
        httpFields.add(this)
    }

    override fun toString(): String = "TrailerBlock(httpFields=$httpFields)"

}

interface AsyncHandler {
    suspend fun handle(ctx: RoutingContext)
}

@HttpServerMarker
class RouterBlock(private val router: Router,
                  private val requestCtx: CoroutineLocal<RoutingContext>?,
                  private val coroutineDispatcher: CoroutineDispatcher) {

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
            value.forEach { router.method(it) }
            field = value
        }

    var path: String = ""
        set(value) {
            router.path(value)
            field = value
        }

    var paths: List<String> = listOf()
        set(value) {
            router.paths(value)
            field = value
        }

    var regexPath: String = ""
        set(value) {
            router.pathRegex(value)
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

    fun getId() = router.id
    /**
     * Register a handler that is executed in the coroutine asynchronously.
     *
     * @param handler The handler that processes the business logic.
     */
    fun asyncHandler(handler: suspend RoutingContext.(context: CoroutineContext) -> Unit) {
        router.handler {
            it.response.isAsynchronous = true
            launch(requestCtx?.createContext(it, coroutineDispatcher) ?: coroutineDispatcher) {
                handler.invoke(it, coroutineContext)
            }
        }
    }

    /**
     * Register a handler that is executed in the coroutine asynchronously.
     *
     * @param handler The handler that processes the business logic.
     */
    fun asyncHandler(handler: AsyncHandler) = asyncHandler {
        handler.handle(this)
    }

    /**
     * Automatically call the succeeded callback when the asynchronous handler has completed.
     */
    fun asyncCompleteHandler(handler: suspend RoutingContext.(context: CoroutineContext) -> Unit) = asyncHandler {
        try {
            handler.invoke(this, it)
            asyncSucceed(Unit)
        } catch (x: Throwable) {
            asyncFail<Unit>(x)
        }
    }

    /**
     * Register a handler that is executed in the network thread synchronously.
     *
     * @param handler The handler that processes the business logic.
     */
    fun handler(handler: RoutingContext.() -> Unit) {
        router.handler(handler)
    }

    /**
     * Register a handler that is executed in the network thread synchronously.
     *
     * @param handler The handler that processes the business logic.
     */
    fun handler(handler: Handler) {
        router.handler(handler)
    }

    /**
     * Automatically close the resource when the block has completed.
     */
    suspend fun <T : Closeable?, R> T.safeUse(block: suspend (T) -> R): R {
        var closed = false
        try {
            return block(this)
        } catch (e: Exception) {
            try {
                withContext(NonCancellable) {
                    closed = true
                    this?.close()
                }
            } catch (closeException: Exception) {
            }
            throw e
        } finally {
            if (!closed) {
                withContext(NonCancellable) {
                    this?.close()
                }
            }
        }
    }

    override fun toString(): String = router.toString()

}

@HttpServerMarker
class WebSocketBlock(server: SimpleHTTPServer,
                     router: Router,
                     private val path: String) : AbstractWebSocketBuilder() {

    var onConnect: ((WebSocketConnection) -> Unit)? = null

    init {
        server.registerWebSocket(path, object : WebSocketHandler {
            override fun onConnect(connection: WebSocketConnection) {
                this@WebSocketBlock.onConnect?.invoke(connection)
            }

            override fun onFrame(frame: Frame, connection: WebSocketConnection) {
                this@WebSocketBlock.onFrame(frame, connection)
            }

            override fun onError(t: Throwable, connection: WebSocketConnection) {
                this@WebSocketBlock.onError(t, connection)
            }
        })
        router.path(path).handler { }
    }

    fun onConnect(onConnect: (WebSocketConnection) -> Unit) {
        this.onConnect = onConnect
    }

    override fun toString(): String = "WebSocket(path='$path')"
}

interface HttpServerLifecycle {

    /**
     * Stop the HTTP server.
     */
    fun stop()

    /**
     * Start the HTTP server.
     *
     * @param host The server hostname.
     * @param port The server port.
     */
    fun listen(host: String, port: Int)

    /**
     * Start the HTTP server and set the address of the local host.
     *
     * @param port The server port.
     */
    fun listen(port: Int)

    /**
     * Start the HTTP server. You must set host and port in the SimpleHTTPServerConfiguration.
     */
    fun listen()
}

@DslMarker
annotation class HttpServerMarker

/**
 * HTTP server DSL. It helps you write HTTP server elegantly.
 *
 * @param requestCtx Maintain the routing context in the HTTP request lifecycle when you use the asynchronous handlers which run in the coroutine.
 * It visits RoutingContext in the external function conveniently. Usually, you can use it to trace HTTP request crossing all registered routers.
 * @param serverConfiguration HTTP server configuration.
 * @param httpBodyConfiguration HTTP body configuration.
 * @param block The HTTP server DSL block. You can register routers in this block.
 */
@HttpServerMarker
class HttpServer(val requestCtx: CoroutineLocal<RoutingContext>? = null,
                 serverConfiguration: SimpleHTTPServerConfiguration = SimpleHTTPServerConfiguration(),
                 httpBodyConfiguration: HTTPBodyConfiguration = HTTPBodyConfiguration(),
                 block: HttpServer.() -> Unit) : HttpServerLifecycle {

    val server = SimpleHTTPServer(serverConfiguration)
    val routerManager = RouterManager.create(httpBodyConfiguration)
    val coroutineDispatcher = Unconfined // server.handlerExecutorService.asCoroutineDispatcher()
    val defaultErrorHandler = DefaultErrorResponseHandlerLoader.getInstance().handler

    init {
        server.badMessage { status, reason, request ->
            val ctx = RoutingContextImpl(request, Collections.emptyNavigableSet<RouterManager.RouterMatchResult>())
            defaultErrorHandler.render(ctx, status, BadMessageException(reason))
        }
        block.invoke(this)
    }

    constructor(coroutineLocal: CoroutineLocal<RoutingContext>?)
            : this(coroutineLocal, SimpleHTTPServerConfiguration(), HTTPBodyConfiguration(), {})

    constructor(coroutineLocal: CoroutineLocal<RoutingContext>?, block: HttpServer.() -> Unit)
            : this(coroutineLocal, SimpleHTTPServerConfiguration(), HTTPBodyConfiguration(), block)

    constructor(coroutineLocal: CoroutineLocal<RoutingContext>?,
                serverConfiguration: SimpleHTTPServerConfiguration,
                httpBodyConfiguration: HTTPBodyConfiguration)
            : this(coroutineLocal, serverConfiguration, httpBodyConfiguration, {})

    constructor(block: HttpServer.() -> Unit) : this(null, block)

    constructor() : this({})

    override fun stop() = server.stop()

    override fun listen(host: String, port: Int) = server.headerComplete(routerManager::accept).listen(host, port)

    override fun listen(port: Int) = listen(InetAddress.getLocalHost().hostAddress, port)

    override fun listen() = server.headerComplete(routerManager::accept).listen()

    fun enableSecureConnection(): HttpServer {
        this.server.configuration.isSecureConnectionEnabled = true
        return this
    }

    /**
     * Register a router using the DSL with a autoincrement ID.
     *
     * @param block The router builder.
     */
    inline fun router(block: RouterBlock.() -> Unit) {
        val r = RouterBlock(routerManager.register(), requestCtx, coroutineDispatcher)
        block.invoke(r)
        sysLogger.info("register $r")
    }

    /**
     * Register a router using the DSL with a specified ID.
     *
     * @param id The router ID. The router is sorted by ID ascending order.
     * @param block The router builder.
     */
    inline fun router(id: Int, block: RouterBlock.() -> Unit) {
        val r = RouterBlock(routerManager.register(id), requestCtx, coroutineDispatcher)
        block.invoke(r)
        sysLogger.info("register $r")
    }

    inline fun webSocket(path: String, block: WebSocketBlock.() -> Unit) {
        val r = WebSocketBlock(server, routerManager.register(), path)
        block.invoke(r)
        sysLogger.info("register $r")
    }

    /**
     * Register routers using the DSL.
     *
     * @param block Register routers in this block.
     */
    inline fun addRouters(block: HttpServer.() -> Unit) = block.invoke(this)
}

fun <T> asyncTraceable(requestCtx: CoroutineLocal<RoutingContext>, context: ContinuationInterceptor = Unconfined, block: suspend CoroutineScope.() -> T): Deferred<T> {
    val ctx = requestCtx.get()
    return if (ctx != null) {
        async(requestCtx.createContext(ctx, context)) { block.invoke(this) }
    } else {
        async(context) { block.invoke(this) }
    }
}