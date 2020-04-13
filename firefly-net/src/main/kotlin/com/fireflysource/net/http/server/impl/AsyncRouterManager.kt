package com.fireflysource.net.http.server.impl

import com.fireflysource.net.http.common.model.HttpMethod
import com.fireflysource.net.http.server.Router
import com.fireflysource.net.http.server.RouterManager
import com.fireflysource.net.http.server.impl.matcher.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class AsyncRouterManager : RouterManager {

    private val routerId = AtomicInteger()
    private val httpMethodMatcher: HttpMethodMatcher = HttpMethodMatcher()
    private val parameterPathMatcher: ParameterPathMatcher = ParameterPathMatcher()
    private val patternedPathMatcher: PatternedPathMatcher = PatternedPathMatcher()
    private val precisePathMatcher: PrecisePathMatcher = PrecisePathMatcher()
    private val regexPathMatcher: RegexPathMatcher = RegexPathMatcher()
    private val acceptHeaderMatcher: AcceptHeaderMatcher = AcceptHeaderMatcher()
    private val preciseContentTypeMatcher: PreciseContentTypeMatcher = PreciseContentTypeMatcher()
    private val patternedContentTypeMatcher: PatternedContentTypeMatcher = PatternedContentTypeMatcher()

    override fun register(): Router = AsyncRouter(routerId.getAndIncrement(), this)

    override fun register(id: Int): Router = AsyncRouter(id, this)

    override fun findRouter(
        method: String,
        path: String,
        contentType: String,
        accept: String
    ): NavigableSet<RouterManager.RouterMatchResult> {
        TODO("Not yet implemented")
    }

    fun method(httpMethod: String, router: AsyncRouter) {
        httpMethodMatcher.add(httpMethod, router)
    }

    fun method(httpMethod: HttpMethod, router: AsyncRouter) {
        httpMethodMatcher.add(httpMethod.value, router)
    }

    fun path(url: String, router: AsyncRouter) {
        when {
            url == "/" -> precisePathMatcher.add(url, router)
            url.contains("*") -> patternedPathMatcher.add(url, router)
            ParameterPathMatcher.isParameterPath(url) -> parameterPathMatcher.add(url, router)
            else -> precisePathMatcher.add(url, router)
        }
    }

    fun paths(urlList: MutableList<String>, router: AsyncRouter) {
        urlList.forEach { path(it, router) }
    }

    fun pathRegex(regex: String, router: AsyncRouter) {
        regexPathMatcher.add(regex, router)
    }

    fun consumes(contentType: String, router: AsyncRouter) {
        if (contentType.contains("*")) patternedContentTypeMatcher.add(contentType, router)
        else preciseContentTypeMatcher.add(contentType, router)
    }

    fun produces(accept: String, router: AsyncRouter) {
        acceptHeaderMatcher.add(accept, router)
    }
}