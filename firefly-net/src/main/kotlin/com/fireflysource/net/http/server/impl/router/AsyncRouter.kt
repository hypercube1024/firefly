package com.fireflysource.net.http.server.impl.router

import com.fireflysource.common.coroutine.CoroutineLocalContext
import com.fireflysource.common.coroutine.asVoidFuture
import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.server.HttpServer
import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Router
import com.fireflysource.net.http.server.Router.EMPTY_HANDLER
import com.fireflysource.net.http.server.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AsyncRouter(
    private val id: Int,
    private val routerManager: AsyncRouterManager
) : Router {

    private val matchTypes = HashSet<Matcher.MatchType>()
    private var handler: Router.Handler = EMPTY_HANDLER
    private var enabled = true

    override fun method(httpMethod: String): Router {
        routerManager.method(httpMethod, this)
        matchTypes.add(Matcher.MatchType.METHOD)
        return this
    }

    override fun method(httpMethod: HttpMethod): Router {
        routerManager.method(httpMethod.value, this)
        matchTypes.add(Matcher.MatchType.METHOD)
        return this
    }

    override fun path(url: String): Router {
        routerManager.path(url, this)
        matchTypes.add(Matcher.MatchType.PATH)
        return this
    }

    override fun paths(urlList: MutableList<String>): Router {
        routerManager.paths(urlList, this)
        matchTypes.add(Matcher.MatchType.PATH)
        return this
    }

    override fun pathRegex(regex: String): Router {
        routerManager.pathRegex(regex, this)
        matchTypes.add(Matcher.MatchType.PATH)
        return this
    }

    override fun get(url: String): Router {
        return method(HttpMethod.GET).path(url)
    }

    override fun post(url: String): Router {
        return method(HttpMethod.POST).path(url)
    }

    override fun put(url: String): Router {
        return method(HttpMethod.PUT).path(url)
    }

    override fun delete(url: String): Router {
        return method(HttpMethod.DELETE).path(url)
    }

    override fun consumes(contentType: String): Router {
        routerManager.consumes(contentType, this)
        matchTypes.add(Matcher.MatchType.CONTENT_TYPE)
        return this
    }

    override fun produces(accept: String): Router {
        routerManager.produces(accept, this)
        matchTypes.add(Matcher.MatchType.ACCEPT)
        return this
    }

    override fun getId(): Int = id

    override fun compareTo(other: Router): Int = id.compareTo(other.id)

    override fun handler(handler: Router.Handler): HttpServer {
        this.handler = handler
        return routerManager.httpServer
    }

    fun getHandler() = this.handler

    override fun getMatchTypes(): MutableSet<Matcher.MatchType> = matchTypes

    override fun enable(): Router {
        enabled = true
        return this
    }

    override fun isEnable(): Boolean = enabled

    override fun disable(): Router {
        enabled = false
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AsyncRouter
        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }

}

private const val serverHandlerCoroutineContextKey = "_serverHandlerCoroutineContextKey"

fun getCurrentRoutingContext(): RoutingContext? = CoroutineLocalContext.getAttr(serverHandlerCoroutineContextKey)

fun Router.asyncHandler(block: suspend CoroutineScope.(RoutingContext) -> Unit): HttpServer {
    return this.handler { ctx ->
        ctx.connection.coroutineScope
            .launch(CoroutineLocalContext.asElement(mutableMapOf(serverHandlerCoroutineContextKey to ctx))) { block(ctx) }
            .asVoidFuture()
    }
}