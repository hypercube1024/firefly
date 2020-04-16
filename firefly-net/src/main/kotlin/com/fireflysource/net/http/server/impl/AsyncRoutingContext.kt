package com.fireflysource.net.http.server.impl

import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.common.model.HttpHeader
import com.fireflysource.net.http.common.model.HttpStatus
import com.fireflysource.net.http.common.model.expectServerAcceptsContent
import com.fireflysource.net.http.server.*
import com.fireflysource.net.http.server.impl.content.provider.DefaultContentProvider
import com.fireflysource.net.http.server.impl.matcher.AbstractPatternMatcher
import com.fireflysource.net.http.server.impl.matcher.AbstractRegexMatcher
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class AsyncRoutingContext(
    private val request: HttpServerRequest,
    private val response: HttpServerResponse,
    private val connection: HttpServerConnection
) : RoutingContext {

    private val attributes: ConcurrentHashMap<String, Any> by lazy { ConcurrentHashMap<String, Any>() }
    var routerMatchResult: RouterManager.RouterMatchResult? = null
    var routerIterator: Iterator<RouterManager.RouterMatchResult>? = null

    override fun getAttribute(key: String): Any? = attributes[key]

    override fun setAttribute(key: String, value: Any): Any? = attributes.put(key, value)

    override fun getAttributes(): MutableMap<String, Any> = attributes

    override fun removeAttribute(key: String): Any? = attributes.remove(key)

    override fun getRequest(): HttpServerRequest = request

    override fun getResponse(): HttpServerResponse = response

    override fun getPathParameter(name: String): String {
        val result = routerMatchResult
        return if (result == null) ""
        else result.parameters[name] ?: ""
    }

    override fun getPathParameter(index: Int): String {
        val result = routerMatchResult
        return if (result == null) ""
        else result.parameters[AbstractPatternMatcher.paramName + index] ?: ""
    }

    override fun getPathParameterByRegexGroup(index: Int): String {
        val result = routerMatchResult
        return if (result == null) ""
        else result.parameters[AbstractRegexMatcher.paramName + index] ?: ""
    }

    override fun expect100Continue(): Boolean {
        return request.httpFields.expectServerAcceptsContent()
    }

    override fun redirect(url: String): CompletableFuture<Void> {
        val status = HttpStatus.FOUND_302
        return setStatus(status)
            .put(HttpHeader.LOCATION, url)
            .contentProvider(DefaultContentProvider(status, null, this))
            .end()
    }

    override fun next(): CompletableFuture<Void> {
        if (!hasNext()) return Result.DONE

        val result = routerIterator?.next()
        return if (result != null) {
            routerMatchResult = result
            val asyncRouter = result.router as AsyncRouter
            asyncRouter.getHandler().apply(this)
        } else Result.DONE
    }

    override fun hasNext(): Boolean {
        return routerIterator?.hasNext() ?: false
    }

    override fun getConnection(): HttpServerConnection = connection

}